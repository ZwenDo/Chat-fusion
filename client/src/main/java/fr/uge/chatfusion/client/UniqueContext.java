package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.FrameOpcodes;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.frame.FrameVisitor;
import fr.uge.chatfusion.core.reader.FrameReaders;
import fr.uge.chatfusion.core.reader.MultiFrameReader;
import fr.uge.chatfusion.core.reader.Reader;
import fr.uge.chatfusion.core.selection.SelectionKeyController;
import fr.uge.chatfusion.core.selection.SelectionKeyControllerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UniqueContext implements SelectionKeyController {
    private static final Logger LOGGER = Logger.getLogger(UniqueContext.class.getName());

    private final SelectionKeyControllerImpl inner;
    private final FrameVisitor<Void> visitor;
    private final InetSocketAddress remoteAddress;

    public UniqueContext(Client client, SelectionKey key, InetSocketAddress remoteAddress) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(key);
        Objects.requireNonNull(remoteAddress);
        this.remoteAddress = remoteAddress;
        this.visitor = new UniqueVisitor(client);
        this.inner = new SelectionKeyControllerImpl(
            key,
            remoteAddress,
            reader(),
            this::onClose,
            this::processIn,
            false
        );
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
    public void doConnect() throws IOException {
        inner.doConnect();
    }

    @Override
    public void queueData(ByteBuffer data) {
        inner.queueData(data);
    }

    @Override
    public void closeWhenAllSent() {
        inner.closeWhenAllSent();
    }

    private void processIn() {
        var bufferIn = inner.bufferIn();
        while (true) {
            try {
                var reader = inner.reader();
                var status = reader.process(bufferIn);
                if (status != Reader.ProcessStatus.DONE) {
                    if (status == Reader.ProcessStatus.ERROR) {
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

    private void logSevereAndClose(String message) {
        LOGGER.log(Level.SEVERE, remoteAddress + " : " + message);
    }

    private void onClose() {
        // do nothing
    }

    private static MultiFrameReader reader() {
        Supplier<Map<Byte, Reader<? extends Frame>>> factory = () -> Map.of(
            FrameOpcodes.LOGIN_ACCEPTED, FrameReaders.loginAcceptedReader(),
            FrameOpcodes.LOGIN_REFUSED, FrameReaders.loginRefusedReader(),
            FrameOpcodes.PUBLIC_MESSAGE, FrameReaders.publicMessageReader()
        );
        return new MultiFrameReader(factory);
    }
}
