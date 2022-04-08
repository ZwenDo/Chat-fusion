package fr.uge.chatfusion.core.frame;

import java.util.Objects;

public record FusionMerge(String name) implements Frame {
    public FusionMerge {
        Objects.requireNonNull(name);
    }

    @Override
    public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
        Objects.requireNonNull(frameVisitor);
        frameVisitor.visit(this, context);
    }
}
