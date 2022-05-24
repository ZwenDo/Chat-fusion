package fr.uge.chatfusion.core.frame;

/**
 * Defines the interface for objects that visits {@link Frame}.
 */
public interface FrameVisitor {

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.AnonymousLogin frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.PublicMessage frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.FusionInit frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.LoginAccepted frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.LoginRefused frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.FusionInitFwd frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.FusionInitOk frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.FusionInitKo frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.FusionChangeLeader frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.FusionMerge frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.FusionRequest frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.DirectMessage frame) {
        throw new UnsupportedOperationException();
    }

    /**
     * Visits the given frame.
     *
     * @param frame the frame to visit
     */
    default void visit(Frame.FileSending frame) {
        throw new UnsupportedOperationException();
    }
}
