package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.frame.*;

import java.util.Objects;

public final class UniqueVisitor implements FrameVisitor<Void> {
    private final Client client;

    public UniqueVisitor(Client client) {
        Objects.requireNonNull(client);
        this.client = client;
    }

    @Override
    public void visit(Frame.LoginAccepted loginAccepted, Void context) {
        Objects.requireNonNull(loginAccepted);
        client.loginAccepted(loginAccepted.serverName());
    }

    @Override
    public void visit(Frame.LoginRefused loginRefused, Void context) {
        Objects.requireNonNull(loginRefused);
        client.loginRefused();
    }

    @Override
    public void visit(Frame.PublicMessage publicMessage, Void context) {
        Objects.requireNonNull(publicMessage);
        System.out.println(publicMessage.format());
    }
}
