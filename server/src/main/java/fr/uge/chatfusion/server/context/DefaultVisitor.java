package fr.uge.chatfusion.server.context;

import fr.uge.chatfusion.core.frame.*;
import fr.uge.chatfusion.server.Server;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Objects;

final class DefaultVisitor implements FrameVisitor<Void> {
    private final SelectionKey key;
    private final Server server;
    private final InetSocketAddress remoteAddress;

    public DefaultVisitor(Server server, SelectionKey key, InetSocketAddress remoteAddress) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(key);
        Objects.requireNonNull(remoteAddress);
        this.server = server;
        this.key = key;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void visit(AnonymousLogin anonymousLogin, Void context) {
        Objects.requireNonNull(anonymousLogin);
        server.tryToConnectAnonymously(anonymousLogin.username(), key);
    }

    @Override
    public void visit(FusionInit fusionInit, Void context) {
        Objects.requireNonNull(fusionInit);
        server.tryFusion(fusionInit, key);
    }

    @Override
    public void visit(FusionMerge fusionMerge, Void context) {
        Objects.requireNonNull(fusionMerge);
        server.mergeFusion(fusionMerge, key);
    }

    @Override
    public void visit(FusionInitOk fusionInitOk, Void context) {
        Objects.requireNonNull(fusionInitOk);
        server.acceptFusion(fusionInitOk, key);
    }

    @Override
    public void visit(FusionInitKo fusionInitKo, Void context) {
        Objects.requireNonNull(fusionInitKo);
        server.rejectFusion(fusionInitKo, key, remoteAddress);
    }

}
