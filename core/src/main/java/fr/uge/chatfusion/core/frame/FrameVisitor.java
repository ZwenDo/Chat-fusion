package fr.uge.chatfusion.core.frame;

public interface FrameVisitor {

    default void visit(Frame.AnonymousLogin anonymousLogin) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.PublicMessage publicMessage) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionInit fusionInit) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.LoginAccepted loginAccepted) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.LoginRefused loginRefused) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionInitFwd fusionInitFwd) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionInitOk fusionInitOk) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionInitKo fusionInitKo) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionChangeLeader fusionChangeLeader) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionMerge fusionMerge) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.FusionRequest fusionRequest) {
        throw new UnsupportedOperationException();
    }

    default void visit(Frame.DirectMessage directMessage) {
        throw new UnsupportedOperationException();
    }
}
