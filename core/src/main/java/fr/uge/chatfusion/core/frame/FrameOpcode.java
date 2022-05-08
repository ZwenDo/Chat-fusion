package fr.uge.chatfusion.core.frame;


import fr.uge.chatfusion.core.reader.Reader;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The opcodes representing the different frames used in the Chatfusion protocol.
 */
enum FrameOpcode {
    /**
     * The opcode for the {@link Frame.AnonymousLogin} frame.
     */
    ANONYMOUS_LOGIN(0, Frame.AnonymousLogin::reader),

    /**
     * The opcode for the {@link Frame.LoginAccepted} frame.
     */
    LOGIN_ACCEPTED(2, Frame.LoginAccepted::reader),

    /**
     * The opcode for the {@link Frame.LoginRefused} frame.
     */
    LOGIN_REFUSED(3, Frame.LoginRefused::reader),

    /**
     * The opcode for the {@link Frame.PublicMessage} frame.
     */
    PUBLIC_MESSAGE(4, Frame.PublicMessage::reader),

    /**
     * The opcode for the {@link Frame.DirectMessage} frame.
     */
    DIRECT_MESSAGE(5, Frame.DirectMessage::reader),

    /**
     * The opcode for the {@link Frame.FileSending} frame.
     */
    FILE_SENDING(6, Frame.FileSending::buffer),

    /**
     * The opcode for the {@link Frame.FusionInit} frame.
     */
    FUSION_INIT(8, Frame.FusionInit::reader),

    /**
     * The opcode for the {@link Frame.FusionInitOk} frame.
     */
    FUSION_INIT_OK(9, Frame.FusionInitOk::reader),

    /**
     * The opcode for the {@link Frame.FusionInitKo} frame.
     */
    FUSION_INIT_KO(10, Frame.FusionInitKo::reader),

    /**
     * The opcode for the {@link Frame.FusionInitOk} frame.
     */
    FUSION_INIT_FWD(11, Frame.FusionInitFwd::reader),

    /**
     * The opcode for the {@link Frame.FusionRequest} frame.
     */
    FUSION_REQUEST(12, Frame.FusionRequest::reader),

    /**
     * The opcode for the {@link Frame.FusionChangeLeader} frame.
     */
    FUSION_CHANGE_LEADER(14, Frame.FusionChangeLeader::reader),

    /**
     * The opcode for the {@link Frame.FusionMerge} frame.
     */
    FUSION_MERGE(15, Frame.FusionMerge::reader),

    ;

    private final byte value;
    private final Function<FrameReaderPart, Reader<? extends Frame>> readerConstructor;

    FrameOpcode(int value, Function<FrameReaderPart, Reader<? extends Frame>> readerConstructor) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Opcode must be between 0 and 255");
        }
        Objects.requireNonNull(readerConstructor);
        this.value = (byte) value;
        this.readerConstructor = readerConstructor;
    }

    /**
     * Gets the enum label corresponding to the given byte opcode.
     *
     * @param value the byte opcode
     * @return the enum label corresponding to the given byte opcode
     */
    public static FrameOpcode get(byte value) {
        var code = map.get(value);
        if (code == null) {
            throw new IllegalArgumentException("Unknown opcode: " + value);
        }
        return code;
    }

    /**
     * Creates a reader which reads the frame of the given opcode.
     *
     * @param part the different parts used to create the reader
     * @return a reader which reads the frame of the given opcode
     */
    @SuppressWarnings("unchecked")
    public Reader<Frame> reader(FrameReaderPart part) {
        Objects.requireNonNull(part);
        return (Reader<Frame>) readerConstructor.apply(part);
    }

    private static final Map<Byte, FrameOpcode> map;
    static {
        map = Arrays.stream(FrameOpcode.values()).collect(Collectors.toUnmodifiableMap(e -> e.value, e -> e));
    }

    /**
     * Gets the byte value of the opcode.
     *
     * @return the byte value of the opcode
     */
    public byte value() {
        return value;
    }
}
