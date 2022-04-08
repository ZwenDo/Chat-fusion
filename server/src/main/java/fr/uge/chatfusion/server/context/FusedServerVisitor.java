package fr.uge.chatfusion.server.context;

import fr.uge.chatfusion.core.frame.*;
import fr.uge.chatfusion.server.ServerToServerInterface;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Objects;

final class FusedServerVisitor implements FrameVisitor<Void> {
    private final ServerToServerInterface server;
    private final SelectionKey key;
    private final InetSocketAddress address;

    FusedServerVisitor(ServerToServerInterface server, SelectionKey key, InetSocketAddress address) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(key);
        Objects.requireNonNull(address);
        this.server = server;
        this.key = key;
        this.address = address;
    }

    @Override
    public void visit(Frame.PublicMessage publicMessage, Void context) {
        Objects.requireNonNull(publicMessage);
        server.forwardPublicMessage(publicMessage);
    }

    @Override
    public void visit(Frame.FusionRequest fusionRequest, Void context) {
        Objects.requireNonNull(fusionRequest);
        System.out.println(fusionRequest.remote());
        server.fusionRequest(fusionRequest, key, address);
    }

    @Override
    public void visit(Frame.FusionChangeLeader fusionChangeLeader, Void context) {
        Objects.requireNonNull(fusionChangeLeader);
        server.changeLeader(fusionChangeLeader, key, address);
    }
}
