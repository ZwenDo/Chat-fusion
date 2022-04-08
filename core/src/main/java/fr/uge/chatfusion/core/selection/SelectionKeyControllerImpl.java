package fr.uge.chatfusion.core.selection;

import fr.uge.chatfusion.core.BufferUtils;
import fr.uge.chatfusion.core.reader.MultiFrameReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SelectionKeyControllerImpl implements SelectionKeyController {
    private static final int BUFFER_SIZE = 1_024;
    private static final Logger LOGGER = Logger.getLogger(SelectionKeyControllerImpl.class.getName());

    private final SelectionKey key;
    private final SocketChannel sc;
    private final InetSocketAddress remoteAddress;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private final ArrayDeque<ByteBuffer> queue = new ArrayDeque<>();
    private MultiFrameReader reader;
    private final Runnable onClose;
    private final Runnable processIn;
    private boolean closing;
    private boolean connected;

    public SelectionKeyControllerImpl(
        SelectionKey key,
        InetSocketAddress remoteAddress,
        MultiFrameReader reader,
        Runnable onClose,
        Runnable processIn,
        boolean isConnected
    ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(remoteAddress);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(onClose);
        Objects.requireNonNull(processIn);
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.remoteAddress = remoteAddress;
        this.reader = reader;
        this.onClose = onClose;
        this.processIn = processIn;
        this.connected = isConnected;
        updateInterestOps();
    }

    @Override
    public void doRead() throws IOException {
        if (sc.read(bufferIn) == -1) {
            logInfoAndClose(sc.getRemoteAddress() + " Connection closed by client.");
            return;
        }

        processIn.run();
        updateInterestOps();
    }

    @Override
    public void doWrite() throws IOException {
        bufferOut.flip();
        sc.write(bufferOut);
        bufferOut.compact();

        processOut();
        updateInterestOps();
    }

    @Override
    public void doConnect() throws IOException {
        if (!sc.finishConnect() || connected) return;
        connected = true;
        updateInterestOps();
    }

    @Override
    public void queueData(ByteBuffer data) {
        Objects.requireNonNull(data);
        if (closing) {
            throw new IllegalStateException("Connection is closing.");
        }

        if (queue.isEmpty()) {
            queue.addLast(ByteBuffer.allocate(BUFFER_SIZE));
        }

        while (data.position() > 0) {
            var dest = queue.peekLast();
            BufferUtils.transferTo(data, dest);
            if (!dest.hasRemaining()) {
                queue.addLast(ByteBuffer.allocate(BUFFER_SIZE));
            }
        }

        processOut();
        updateInterestOps();
    }

    @Override
    public void closeWhenAllSent() {
        closing = true;
        updateInterestOps();
    }


    private void updateInterestOps() {
        if (!connected) {
            key.interestOps(SelectionKey.OP_CONNECT);
            return;
        }

        var op = 0;

        if (!closing && bufferIn.hasRemaining()) {
            op |= SelectionKey.OP_READ;
        }

        if (bufferOut.position() > 0) {
            op |= SelectionKey.OP_WRITE;
        }

        if (op == 0) {
            logInfoAndClose("No more interest ops. Closing connection.");
            return;
        }

        key.interestOps(op);
    }

    private void processOut() {
        while (!queue.isEmpty() && bufferOut.hasRemaining()) {
            var data = queue.peekFirst();
            if (data.position() == 0) {
                queue.removeFirst();
                continue;
            }
            BufferUtils.transferTo(data, bufferOut);
        }
    }

    private void logInfoAndClose(String message) {
        LOGGER.log(Level.INFO, remoteAddress + " : " + message);
        onClose.run();
    }

    public void setReader(MultiFrameReader reader) {
        Objects.requireNonNull(reader);
        this.reader = reader;
    }

    public MultiFrameReader reader() {
        return reader;
    }

    public ByteBuffer bufferIn() {
        return bufferIn;
    }
}
