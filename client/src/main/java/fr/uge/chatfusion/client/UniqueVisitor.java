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
    public void visit(Frame.LoginAccepted loginAccepted) {
        Objects.requireNonNull(loginAccepted);
        client.loginAccepted(loginAccepted.serverName());
    }

    @Override
    public void visit(Frame.LoginRefused loginRefused) {
        Objects.requireNonNull(loginRefused);
        client.loginRefused();
    }

    @Override
    public void visit(Frame.PublicMessage publicMessage) {
        Objects.requireNonNull(publicMessage);
        System.out.println(publicMessage.format());
    }

    @Override
    public void visit(Frame.DirectMessage directMessage) {
        Objects.requireNonNull(directMessage);
        System.out.println(directMessage.format());
    }

    @Override
    public void visit(Frame.FileSending fileSending) {
        Objects.requireNonNull(fileSending);
        client.receiveFileBlock(fileSending);
    }
}
