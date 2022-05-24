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
    public void visit(Frame.PublicMessage frame) {
        Objects.requireNonNull(frame);
        server.forwardPublicMessage(frame, infos);
    }

    @Override
    public void visit(Frame.FusionRequest frame) {
        Objects.requireNonNull(frame);
        System.out.println(frame.remote());
        server.fusionRequest(frame, infos);
    }

    @Override
    public void visit(Frame.FusionChangeLeader frame) {
        Objects.requireNonNull(frame);
        server.changeLeader(frame, infos);
    }

    @Override
    public void visit(Frame.DirectMessage frame) {
        Objects.requireNonNull(frame);
        server.sendDirectMessage(frame, infos);
    }

    @Override
    public void visit(Frame.FileSending frame) {
        Objects.requireNonNull(frame);
        server.sendFile(frame, infos);
    }
}
