package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.frame.FrameVisitor;

import java.util.Objects;

final class UniqueVisitor implements FrameVisitor {
    private final Client client;

    public UniqueVisitor(Client client) {
        Objects.requireNonNull(client);
        this.client = client;
    }

    @Override
    public void visit(Frame.LoginAccepted frame) {
        Objects.requireNonNull(frame);
        client.loginAccepted(frame.serverName());
    }

    @Override
    public void visit(Frame.LoginRefused frame) {
        Objects.requireNonNull(frame);
        client.loginRefused();
    }

    @Override
    public void visit(Frame.PublicMessage frame) {
        Objects.requireNonNull(frame);
        client.receivePublicMessage(frame);
    }

    @Override
    public void visit(Frame.DirectMessage frame) {
        Objects.requireNonNull(frame);
        client.receiveDirectMessage(frame);
    }

    @Override
    public void visit(Frame.FileSending frame) {
        Objects.requireNonNull(frame);
        client.receiveFileBlock(frame);
    }
}
