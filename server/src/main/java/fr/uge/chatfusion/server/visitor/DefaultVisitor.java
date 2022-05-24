package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.frame.FrameVisitor;

import java.util.Objects;

final class DefaultVisitor implements FrameVisitor {
    private final DefaultToServerInterface server;
    private final UnknownRemoteInfo infos;

    public DefaultVisitor(DefaultToServerInterface server, UnknownRemoteInfo infos) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(infos);
        this.server = server;
        this.infos = infos;
    }

    @Override
    public void visit(Frame.AnonymousLogin frame) {
        Objects.requireNonNull(frame);
        server.connectAnonymously(frame, infos);
    }

    @Override
    public void visit(Frame.FusionInit frame) {
        Objects.requireNonNull(frame);
        server.tryFusion(frame, infos);
    }

    @Override
    public void visit(Frame.FusionMerge frame) {
        Objects.requireNonNull(frame);
        server.fusionMerge(frame, infos);
    }

}
