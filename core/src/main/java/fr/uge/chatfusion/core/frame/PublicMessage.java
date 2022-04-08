package fr.uge.chatfusion.core.frame;

import java.util.Objects;

public record PublicMessage(String originServer, String senderUsername, String message) implements Frame {
    public PublicMessage {
        Objects.requireNonNull(originServer);
        Objects.requireNonNull(senderUsername);
        Objects.requireNonNull(message);
    }

    @Override
    public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
        Objects.requireNonNull(frameVisitor);
        frameVisitor.visit(this, context);
    }

    public String format() {
        return "[" + originServer + "] " + senderUsername + ": " + message;
    }
}
