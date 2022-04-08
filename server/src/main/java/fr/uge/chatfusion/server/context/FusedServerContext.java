package fr.uge.chatfusion.server.context;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.FrameOpcodes;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.reader.FrameReaders;
import fr.uge.chatfusion.core.reader.MultiFrameReader;
import fr.uge.chatfusion.core.reader.base.Reader;
import fr.uge.chatfusion.server.ServerToServerInterface;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class FusedServerContext extends AbstractContext implements Context {
    private final ServerToServerInterface server;
    private final String serverName;

    FusedServerContext(DefaultContext defaultContext, ServerToServerInterface server, String serverName) {
        super(defaultContext, new FusedServerVisitor(server, defaultContext.key(), defaultContext.remoteAddress()));
        Objects.requireNonNull(server);
        Objects.requireNonNull(serverName);
        inner.setReader(reader());
        this.server = server;
        this.serverName = serverName;
    }

    public FusedServerContext(
        ServerToServerInterface server,
        String serverName,
        SelectionKey key,
        InetSocketAddress address,
        boolean connected
    ) {
        super(key, address, reader(), connected);
        Objects.requireNonNull(server);
        Objects.requireNonNull(serverName);
        this.server = server;
        this.serverName = serverName;
        this.visitor = new FusedServerVisitor(server, key, address);
    }

    @Override
    protected void onClose() {
        CloseableUtils.silentlyClose(sc);
    }

    private static MultiFrameReader reader() {
        Supplier<Map<Byte, Reader<? extends Frame>>> factory = () -> Map.of(
            FrameOpcodes.PUBLIC_MESSAGE, FrameReaders.publicMessageReader(),
            FrameOpcodes.FUSION_REQUEST, FrameReaders.fusionRequestReader(),
            FrameOpcodes.FUSION_CHANGE_LEADER, FrameReaders.fusionChangeLeaderReader()
        );
        return new MultiFrameReader(factory);
    }

    public String serverName() {
        return serverName;
    }
}
