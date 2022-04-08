package fr.uge.chatfusion.core.frame;

import java.net.InetSocketAddress;
import java.util.Objects;

public record FusionRequest(InetSocketAddress remote) implements Frame {
    public FusionRequest {
        Objects.requireNonNull(remote);
    }

    @Override
    public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
        Objects.requireNonNull(frameVisitor);
        frameVisitor.visit(this, context);
    }
}
