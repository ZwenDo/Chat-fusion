package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.frame.FrameVisitor;

import java.util.Objects;

final class FusedServerVisitor implements FrameVisitor {
    private final ServerToServerInterface server;
    private final IdentifiedRemoteInfo infos;

    FusedServerVisitor(ServerToServerInterface server, IdentifiedRemoteInfo infos) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(infos);
        this.server = server;
        this.infos = infos;
    }

    @Override
    public void visit(Frame.PublicMessage publicMessage) {
        Objects.requireNonNull(publicMessage);
        server.forwardPublicMessage(publicMessage, infos);
    }

    @Override
    public void visit(Frame.FusionRequest fusionRequest) {
        Objects.requireNonNull(fusionRequest);
        System.out.println(fusionRequest.remote());
        server.fusionRequest(fusionRequest, infos);
    }

    @Override
    public void visit(Frame.FusionChangeLeader fusionChangeLeader) {
        Objects.requireNonNull(fusionChangeLeader);
        server.changeLeader(fusionChangeLeader, infos);
    }

    @Override
    public void visit(Frame.DirectMessage directMessage) {
        Objects.requireNonNull(directMessage);
        server.sendDirectMessage(directMessage, infos);
    }
}
