package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.BufferUtils;
import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.Opcodes;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.reader.FrameReaders;
import fr.uge.chatfusion.core.reader.MultiFrameReader;
import fr.uge.chatfusion.core.reader.Reader;
import fr.uge.chatfusion.server.processor.FrameProcessorClient;
import fr.uge.chatfusion.server.processor.PreLogClientProcessor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ClientContext {
    private static final int BUFFER_SIZE = 1_024;
    private static final Logger LOGGER = Logger.getLogger(ClientContext.class.getName());

    private final SelectionKey key;
    private final SocketChannel sc;
    private final InetSocketAddress remoteAddress;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private final ArrayDeque<ByteBuffer> queue = new ArrayDeque<>();
    private final MultiFrameReader reader = reader();
    private final ServerInterface server;
    private FrameProcessorClient currentProcessor;

    public ClientContext(ServerInterface server, SelectionKey key) throws IOException {
        Objects.requireNonNull(server);
        Objects.requireNonNull(key);
        this.server = server;
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.remoteAddress = (InetSocketAddress) sc.getRemoteAddress();
        this.currentProcessor = new PreLogClientProcessor(server, sc, remoteAddress);
    }

    /**
     * Performs the read action on sc
     * <p>
     * The convention is that both buffers are in write-mode before the call to
     * doRead and after the call
     *
     * @throws IOException
     */
    public void doRead() throws IOException {
        if (sc.read(bufferIn) == -1) {
            logAndClose(Level.INFO, sc.getRemoteAddress() + " Connection closed by client.");
        }

        processIn();
        updateInterestOps();
    }

    public void doWrite() throws IOException {
        bufferOut.flip();
        sc.write(bufferOut);
        bufferOut.compact();

        processOut();
        updateInterestOps();
    }

    public void queueData(ByteBuffer data) {
        Objects.requireNonNull(data);
        if (queue.isEmpty()) {
            queue.addLast(ByteBuffer.allocate(BUFFER_SIZE));
        }

        while (data.hasRemaining()) {
            var dest = queue.peekLast();
            BufferUtils.transferTo(data, dest);
            if (!dest.hasRemaining()) {
                queue.addLast(ByteBuffer.allocate(BUFFER_SIZE));
            }
        }
    }

    private void updateInterestOps() {
        var op = 0;

        if (bufferIn.hasRemaining()) {
            op |= SelectionKey.OP_READ;
        }

        if (bufferOut.position() > 0) {
            op |= SelectionKey.OP_WRITE;
        }

        if (op == 0) {
            logAndClose(Level.WARNING, "No more interest ops. Closing connection.");
            return;
        }

        key.interestOps(op);
    }

    private void processIn() {
        while (true) {
            try {
                var status = reader.process(bufferIn);
                if (status != Reader.ProcessStatus.DONE) {
                    if (status == Reader.ProcessStatus.ERROR) {
                        logAndClose(Level.SEVERE, "Malformed message packet. Closing connection.");
                    }
                    break;
                }

                reader.get().accept(currentProcessor, null);
                reader.reset();
                currentProcessor = currentProcessor.nextProcessor();
            } catch (IllegalStateException e) {
                logAndClose(Level.SEVERE, "Error while reading. Closing connection...\n" + e.getMessage());
            }
        }
    }

    /**
     * Try to fill bufferOut from the data queue
     */
    private void processOut() {
        while (!queue.isEmpty() && bufferOut.hasRemaining()) {
            var data = queue.peekFirst();
            if (!data.hasRemaining()) {
                queue.removeFirst();
                continue;
            }
            BufferUtils.transferTo(data, bufferOut);
        }
    }

    private static MultiFrameReader reader() {
        Supplier<Map<Byte, Reader<? extends Frame>>> factory = () -> Map.of(
            Opcodes.ANONYMOUS_LOGIN.value(), FrameReaders.anonymousLoginReader(),
            Opcodes.PUBLIC_MESSAGE.value(), FrameReaders.publicMessageReader()
        );
        return new MultiFrameReader(factory);
    }

    private void logAndClose(Level level, String message) {
        CloseableUtils.logForRemoteAndSilentlyClose(LOGGER, level, message, remoteAddress, sc);
    }
}
