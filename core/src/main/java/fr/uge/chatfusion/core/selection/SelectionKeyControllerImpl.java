package fr.uge.chatfusion.core.selection;

import fr.uge.chatfusion.core.BufferUtils;
import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.frame.FrameVisitor;
import fr.uge.chatfusion.core.reader.base.Reader;

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
    private final Reader<Frame> reader = Frame.reader();
    private Runnable onClose = () -> {};
    private FrameVisitor visitor = new FrameVisitor() {};
    private boolean closing;
    private boolean closed;
    private boolean connected;

    public SelectionKeyControllerImpl(SelectionKey key, InetSocketAddress remoteAddress, boolean isConnected) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(remoteAddress);
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.remoteAddress = remoteAddress;
        this.connected = isConnected;
        updateInterestOps();
    }

    @Override
    public void doRead() throws IOException {
        if (sc.read(bufferIn) == -1) {
            logAndClose(Level.INFO, sc.getRemoteAddress() + " Connection closed by client.");
            return;
        }

        processIn();
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
        if (!sc.finishConnect() || connected || closed) return;
        connected = true;
        updateInterestOps();
    }

    @Override
    public void queueData(ByteBuffer data) {
        Objects.requireNonNull(data);
        if (closing || closed) {
            throw new IllegalStateException("Connection is closing or closed.");
        }

        if (queue.isEmpty()) {
            queue.addLast(ByteBuffer.allocate(BUFFER_SIZE));
        }

        while (data.position() > 0) {
            var dest = queue.getLast();
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

    @Override
    public void close() {
        CloseableUtils.silentlyClose(sc);
        closed = true;
        onClose.run();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    public void setVisitor(FrameVisitor visitor) {
        Objects.requireNonNull(visitor);
        this.visitor = visitor;
    }

    public void setOnClose(Runnable onClose) {
        Objects.requireNonNull(onClose);
        this.onClose = onClose;
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
            logAndClose(Level.INFO, "No more interest ops. Closing connection.");
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

                reader.get().accept(visitor);
                reader.reset();
            } catch (IllegalStateException e) {
                logAndClose(Level.SEVERE,"Error while reading. Closing connection...\n" + e.getMessage());
                break;
            }
        }
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

    private void logAndClose(Level level, String message) {
        LOGGER.log(level, remoteAddress + " : " + message);
        CloseableUtils.silentlyClose(sc);
        onClose.run();
    }

}
