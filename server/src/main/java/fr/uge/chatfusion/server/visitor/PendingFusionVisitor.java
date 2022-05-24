package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.frame.FrameVisitor;

import java.util.Objects;

final class PendingFusionVisitor implements FrameVisitor {
    private final PendingFusionToServerInterface server;
    private final UnknownRemoteInfo infos;

    public PendingFusionVisitor(PendingFusionToServerInterface server, UnknownRemoteInfo infos) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(infos);
        this.server = server;
        this.infos = infos;
    }

    @Override
    public void visit(Frame.FusionInitOk frame) {
        Objects.requireNonNull(frame);
        server.fusionAccepted(frame, infos);
    }

    @Override
    public void visit(Frame.FusionInitKo frame) {
        Objects.requireNonNull(frame);
        server.fusionRejected(frame, infos);
    }

    @Override
    public void visit(Frame.FusionInitFwd frame) {
        Objects.requireNonNull(frame);
        server.fusionForwarded(frame, infos);
    }
}
