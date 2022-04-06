package fr.uge.chatfusion.core.frame;

public interface Frame {
    <C> void accept(FrameVisitor<C> frameVisitor, C context);
}
