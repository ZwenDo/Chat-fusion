package fr.uge.chatfusion.core.selection;

import fr.uge.chatfusion.core.base.BufferUtils;
import fr.uge.chatfusion.core.base.CloseableUtils;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.frame.FrameVisitor;
import fr.uge.chatfusion.core.reader.Reader;

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
    private static final int BUFFER_SIZE = 2_048; // 2 KB
    private static final Logger LOGGER = Logger.getLogger(SelectionKeyControllerImpl.class.getName());

    private final SelectionKey key;
    private final SocketChannel sc;
    private final InetSocketAddress remoteAddress;
    private final ByteBuffer bufferIn;
    private final ByteBuffer bufferOut;
    private final ArrayDeque<ByteBuffer> queue = new ArrayDeque<>();
    private final Reader<Frame> reader = Frame.reader();
    private final boolean logging;
    private Runnable onClose = () -> {
    };
    private Runnable onSendingAllData = () -> {
    };
    private FrameVisitor visitor = new FrameVisitor() {
    };
    private boolean closing;
    private boolean connected;

    public SelectionKeyControllerImpl(
        SelectionKey key,
        InetSocketAddress remoteAddress,
        boolean isConnected,
        boolean logging,
        boolean isDirect
    ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(remoteAddress);
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.remoteAddress = remoteAddress;
        this.connected = isConnected;
        this.logging = logging;
        if (isDirect) {
            bufferIn = ByteBuffer.allocateDirect(BUFFER_SIZE);
            bufferOut = ByteBuffer.allocateDirect(BUFFER_SIZE);
        } else {
            bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
            bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
        }
        updateInterestOps();
    }

    @Override
    public void doRead() throws IOException {
        if (sc.read(bufferIn) == -1) {
            logAndClose(Level.INFO, " Connection closed remotely.");
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
        if (bufferOut.position() == 0) {
            onSendingAllData.run();
        }
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
            throw new IllegalStateException("Connection is closing or closed.");
        }
        BufferUtils.fillQueue(queue, data, BUFFER_SIZE);
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

    public void setOnClose(Runnable onClose) {
        Objects.requireNonNull(onClose);
        this.onClose = onClose;
    }

    public void setOnSendingAllData(Runnable onSendingAllData) {
        Objects.requireNonNull(onSendingAllData);
        this.onSendingAllData = onSendingAllData;
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
                logAndClose(Level.SEVERE, "Error while reading. Closing connection...\n" + e.getMessage());
                break;
            } catch (UnsupportedOperationException e) {
                logAndClose(
                    Level.SEVERE,
                    "Reader cannot read "
                        + reader.get().getClass().getSimpleName()
                        + ". Closing connection...\n"
                );
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
        if (logging) {
            LOGGER.log(level, remoteAddress + " : " + message);
        }
        CloseableUtils.silentlyClose(sc);
        onClose.run();
    }

}
