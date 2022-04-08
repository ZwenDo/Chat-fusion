package fr.uge.chatfusion.core.frame;

import java.util.Objects;

public record FusionInitKo() implements Frame {
    @Override
    public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
        Objects.requireNonNull(frameVisitor);
        frameVisitor.visit(this, context);
    }
}
