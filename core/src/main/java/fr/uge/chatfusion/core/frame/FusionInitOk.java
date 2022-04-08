package fr.uge.chatfusion.core.frame;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

public record FusionInitOk(String serverName, InetSocketAddress serverAddress, List<ServerInfo> members) implements Frame {
    public FusionInitOk(String serverName, InetSocketAddress serverAddress, List<ServerInfo> members) {
        Objects.requireNonNull(serverName);
        Objects.requireNonNull(serverAddress);
        Objects.requireNonNull(members);
        this.serverName = serverName;
        this.serverAddress = serverAddress;
        this.members = List.copyOf(members);
    }

    @Override
    public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
        Objects.requireNonNull(frameVisitor);
        frameVisitor.visit(this, context);
    }
}
