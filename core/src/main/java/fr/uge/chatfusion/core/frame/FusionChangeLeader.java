package fr.uge.chatfusion.core.frame;

import java.net.InetSocketAddress;
import java.util.Objects;

public record FusionChangeLeader(String leaderName, InetSocketAddress leaderAddress) implements Frame {
    public FusionChangeLeader {
        Objects.requireNonNull(leaderName);
        Objects.requireNonNull(leaderAddress);
    }

    @Override
    public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
        Objects.requireNonNull(frameVisitor);
        frameVisitor.visit(this, context);
    }
}
