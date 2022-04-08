package fr.uge.chatfusion.server.context;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.FrameOpcodes;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.reader.FrameReaders;
import fr.uge.chatfusion.core.reader.MultiFrameReader;
import fr.uge.chatfusion.core.reader.Reader;
import fr.uge.chatfusion.server.Server;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultContext extends AbstractContext implements Context {
    private final Server server;

    public DefaultContext(Server server, SelectionKey key, InetSocketAddress remoteAddress, boolean connected) {
        super(key, remoteAddress, reader(), connected);
        Objects.requireNonNull(server);
        visitor = new DefaultVisitor(server, key, remoteAddress);
        this.server = server;
    }

    @Override
    protected void onClose() {
        CloseableUtils.silentlyClose(sc);
    }

    private static MultiFrameReader reader() {
        Supplier<Map<Byte, Reader<? extends Frame>>> factory = () -> Map.of(
            FrameOpcodes.ANONYMOUS_LOGIN, FrameReaders.anonymousLoginReader(),
            FrameOpcodes.FUSION_INIT, FrameReaders.fusionInitReader(),
            FrameOpcodes.FUSION_INIT_OK, FrameReaders.fusionInitOkReader(),
            FrameOpcodes.FUSION_INIT_KO, FrameReaders.fusionInitKoReader(),
            FrameOpcodes.FUSION_MERGE, FrameReaders.fusionMergeReader()
        );
        return new MultiFrameReader(factory);
    }

    public LoggedClientContext asClientContext(String name) {
        Objects.requireNonNull(name);
        return new LoggedClientContext(this, server, name);
    }

    public FusedServerContext asServerContext(String name) {
        Objects.requireNonNull(name);
        return new FusedServerContext(this, server, name);
    }

}
