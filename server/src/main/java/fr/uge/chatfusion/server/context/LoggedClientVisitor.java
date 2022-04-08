package fr.uge.chatfusion.server.context;

import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.frame.FrameVisitor;
import fr.uge.chatfusion.server.ClientToServerInterface;

import java.util.Objects;

final class LoggedClientVisitor implements FrameVisitor<Void> {
    private final ClientToServerInterface server;
    private final String username;

    public LoggedClientVisitor(ClientToServerInterface server, String username) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(username);
        this.server = server;
        this.username = username;
    }

    @Override
    public void visit(Frame.PublicMessage publicMessage, Void context) {
        Objects.requireNonNull(publicMessage);
        server.sendPublicMessage(publicMessage);
    }


}
