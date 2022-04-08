package fr.uge.chatfusion.core.frame;

import java.net.InetSocketAddress;
import java.util.Objects;

public record FusionInitFwd(InetSocketAddress leaderAddress) implements Frame {
    public FusionInitFwd {
        Objects.requireNonNull(leaderAddress);
    }

    @Override
    public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
        Objects.requireNonNull(frameVisitor);
        frameVisitor.visit(this, context);
    }
}
