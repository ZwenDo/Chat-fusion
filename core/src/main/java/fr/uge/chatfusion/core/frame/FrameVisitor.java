package fr.uge.chatfusion.core.frame;

public interface FrameVisitor<C> {

    default void visit(AnonymousLogin anonymousLogin, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(PublicMessage publicMessage, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionInit fusionInit, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(LoginAccepted loginAccepted, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(LoginRefused loginRefused, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionInitFwd fusionInitFwd, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionInitOk fusionInitOk, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionInitKo fusionInitKo, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionChangeLeader fusionChangeLeader, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionMerge fusionMerge, C context) {
        throw new UnsupportedOperationException();
    }

    default void visit(FusionRequest fusionRequest, C context) {
        throw new UnsupportedOperationException();
    }
}
