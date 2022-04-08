package fr.uge.chatfusion.server.context;

import fr.uge.chatfusion.core.FrameOpcodes;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.reader.FrameReaders;
import fr.uge.chatfusion.core.reader.MultiFrameReader;
import fr.uge.chatfusion.core.reader.base.Reader;
import fr.uge.chatfusion.server.ClientToServerInterface;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class LoggedClientContext extends AbstractContext implements Context {
    private final ClientToServerInterface server;
    private final String username;


    LoggedClientContext(DefaultContext defaultContext, ClientToServerInterface server, String username) {
        super(defaultContext, new LoggedClientVisitor(server, username));
        Objects.requireNonNull(server);
        Objects.requireNonNull(username);
        inner.setReader(reader());
        this.server = server;
        this.username = username;
    }

    @Override
    protected void onClose() {
        server.disconnectClient(username);
    }

    private static MultiFrameReader reader() {
        Supplier<Map<Byte, Reader<? extends Frame>>> factory = () -> Map.of(
            FrameOpcodes.PUBLIC_MESSAGE, FrameReaders.publicMessageReader()
        );
        return new MultiFrameReader(factory);
    }
}
