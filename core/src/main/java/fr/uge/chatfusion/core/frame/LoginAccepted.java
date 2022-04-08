package fr.uge.chatfusion.core.frame;

import java.util.Objects;

public record LoginAccepted(String serverName) implements Frame {
    public LoginAccepted {
        Objects.requireNonNull(serverName);
    }

    @Override
    public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
        Objects.requireNonNull(frameVisitor);
        frameVisitor.visit(this, context);
    }
}
