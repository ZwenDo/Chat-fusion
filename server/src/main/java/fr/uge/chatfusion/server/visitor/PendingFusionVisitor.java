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
    public void visit(Frame.FusionInitOk fusionInitOk) {
        Objects.requireNonNull(fusionInitOk);
        server.fusionAccepted(fusionInitOk, infos);
    }

    @Override
    public void visit(Frame.FusionInitKo fusionInitKo) {
        Objects.requireNonNull(fusionInitKo);
        server.fusionRejected(fusionInitKo, infos);
    }

    @Override
    public void visit(Frame.FusionInitFwd fusionInitFwd) {
        Objects.requireNonNull(fusionInitFwd);
        server.fusionForwarded(fusionInitFwd, infos);
    }
}
