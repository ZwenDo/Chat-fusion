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
    public void visit(Frame.AnonymousLogin anonymousLogin) {
        Objects.requireNonNull(anonymousLogin);
        server.connectAnonymously(anonymousLogin, infos);
    }

    @Override
    public void visit(Frame.FusionInit fusionInit) {
        Objects.requireNonNull(fusionInit);
        server.tryFusion(fusionInit, infos);
    }

    @Override
    public void visit(Frame.FusionMerge fusionMerge) {
        Objects.requireNonNull(fusionMerge);
        server.fusionMerge(fusionMerge, infos);
    }

}
