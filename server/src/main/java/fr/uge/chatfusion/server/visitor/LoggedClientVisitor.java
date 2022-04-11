package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.frame.FrameVisitor;

import java.util.Objects;

final class LoggedClientVisitor implements FrameVisitor {
    private final ClientToServerInterface server;
    private final IdentifiedRemoteInfo infos;

    public LoggedClientVisitor(
        ClientToServerInterface server,
        IdentifiedRemoteInfo infos
    ) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(infos);
        this.server = server;
        this.infos = infos;
    }

    @Override
    public void visit(Frame.PublicMessage publicMessage) {
        Objects.requireNonNull(publicMessage);
        server.sendPublicMessage(publicMessage, infos);
    }


}
