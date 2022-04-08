package fr.uge.chatfusion.core.frame;

public record LoginRefused() implements Frame{
    @Override
    public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
        frameVisitor.visit(this, context);
    }
}
