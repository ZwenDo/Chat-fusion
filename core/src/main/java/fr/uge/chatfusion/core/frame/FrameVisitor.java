package fr.uge.chatfusion.core.frame;

public interface FrameVisitor<C> {

    default void visit(Frame.AnonymousLogin anonymousLogin, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.PublicMessage publicMessage, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionInit fusionInit, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.LoginAccepted loginAccepted, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.LoginRefused loginRefused, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionInitFwd fusionInitFwd, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionInitOk fusionInitOk, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionInitKo fusionInitKo, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionChangeLeader fusionChangeLeader, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionMerge fusionMerge, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionRequest fusionRequest, C context) {
        throw new UnsupportedOperationException();
    }
}
