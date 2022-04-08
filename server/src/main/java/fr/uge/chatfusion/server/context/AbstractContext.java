package fr.uge.chatfusion.server.context;

import fr.uge.chatfusion.core.frame.FrameVisitor;
import fr.uge.chatfusion.core.reader.MultiFrameReader;
import fr.uge.chatfusion.core.reader.Reader;
import fr.uge.chatfusion.core.selection.SelectionKeyController;
import fr.uge.chatfusion.core.selection.SelectionKeyControllerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class AbstractContext implements SelectionKeyController {
    private static final Logger LOGGER = Logger.getLogger(AbstractContext.class.getName());

    protected final SelectionKeyControllerImpl inner;
    protected FrameVisitor<Void> visitor;
    protected final SelectionKey key;
    protected final SocketChannel sc;
    protected final InetSocketAddress remoteAddress;

    protected AbstractContext(
        SelectionKey key,
        InetSocketAddress remoteAddress,
        MultiFrameReader reader,
        boolean connected
    ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(remoteAddress);
        Objects.requireNonNull(reader);
        this.inner = new SelectionKeyControllerImpl(
            key,
            remoteAddress,
            reader,
            this::onClose,
            this::processIn,
            connected
        );
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.remoteAddress = remoteAddress;
    }

    protected AbstractContext(AbstractContext other, FrameVisitor<Void> visitor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(visitor);
        this.inner = other.inner;
        this.visitor = visitor;
        this.key = other.key;
        this.sc = other.sc;
        this.remoteAddress = other.remoteAddress;
        other.visitor = visitor; // TODO find a way to avoid this
    }

    @Override
    public void doRead() throws IOException {
        inner.doRead();
    }

    @Override
    public void doWrite() throws IOException {
        inner.doWrite();
    }

    @Override
    public void closeWhenAllSent() {
        inner.closeWhenAllSent();
    }

    @Override
    public void queueData(ByteBuffer data) {
        inner.queueData(data);
    }

    public SocketChannel socketChannel() {
        return sc;
    }

    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    public SelectionKey key() {
        return key;
    }

    protected abstract void onClose();

    private void processIn() {
        var bufferIn = inner.bufferIn();
        while (true) {
            try {
                var reader = inner.reader();
                var status = reader.process(bufferIn);
                if (status != Reader.ProcessStatus.DONE) {
                    if (status == Reader.ProcessStatus.ERROR) {
                        System.out.println(reader);
                        logSevereAndClose("Malformed message packet. Closing connection.");
                    }
                    break;
                }

                reader.get().accept(visitor, null);
                reader.reset();
            } catch (IllegalStateException e) {
                logSevereAndClose("Error while reading. Closing connection...\n" + e.getMessage());
                break;
            }
        }
    }

    @Override
    public void doConnect() throws IOException {
        inner.doConnect();
    }

    private void logSevereAndClose(String message) {
        LOGGER.log(Level.SEVERE, remoteAddress + " : " + message);
        onClose();
    }
}
