package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.frame.FrameVisitor;
import fr.uge.chatfusion.core.frame.LoginAccepted;
import fr.uge.chatfusion.core.frame.LoginRefused;
import fr.uge.chatfusion.core.frame.PublicMessage;

import java.util.Objects;

public final class UniqueVisitor implements FrameVisitor<Void> {
    private final Client client;

    public UniqueVisitor(Client client) {
        Objects.requireNonNull(client);
        this.client = client;
    }

    @Override
    public void visit(LoginAccepted loginAccepted, Void context) {
        Objects.requireNonNull(loginAccepted);
        client.loginAccepted(loginAccepted.serverName());
    }

    @Override
    public void visit(LoginRefused loginRefused, Void context) {
        Objects.requireNonNull(loginRefused);
        client.loginRefused();
    }

    @Override
    public void visit(PublicMessage publicMessage, Void context) {
        Objects.requireNonNull(publicMessage);
        System.out.println(publicMessage.format());
    }
}
