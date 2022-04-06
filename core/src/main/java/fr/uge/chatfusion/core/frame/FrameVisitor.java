package fr.uge.chatfusion.core.frame;

public interface FrameVisitor<C> {

    default void visit(AnonymousLogin anonymousLogin, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(PublicMessage publicMessage, C context) {
        throw new UnsupportedOperationException();
    }
}
