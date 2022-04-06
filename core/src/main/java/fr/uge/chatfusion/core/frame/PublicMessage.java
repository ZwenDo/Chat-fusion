package fr.uge.chatfusion.core.frame;

import java.util.Objects;

public record PublicMessage(String message) implements Frame {
    public PublicMessage {
        Objects.requireNonNull(message);
    }

    @Override
    public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
        Objects.requireNonNull(frameVisitor);
        frameVisitor.visit(this, context);
    }
}
