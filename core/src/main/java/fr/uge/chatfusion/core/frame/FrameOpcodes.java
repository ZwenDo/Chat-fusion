package fr.uge.chatfusion.core.frame;

import fr.uge.chatfusion.core.reader.base.Reader;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

enum FrameOpcodes {
    ANONYMOUS_LOGIN(0, Frame.AnonymousLogin::reader),
    LOGIN_ACCEPTED(2, Frame.LoginAccepted::reader),
    LOGIN_REFUSED(3, Frame.LoginRefused::reader),
    PUBLIC_MESSAGE(4, Frame.PublicMessage::reader),
    FUSION_INIT(8, Frame.FusionInit::reader),
    FUSION_INIT_FWD(11, Frame.FusionInitFwd::reader),
    FUSION_INIT_OK(9, Frame.FusionInitOk::reader),
    FUSION_INIT_KO(10, Frame.FusionInitKo::reader),
    FUSION_REQUEST(12, Frame.FusionRequest::reader),
    FUSION_CHANGE_LEADER(14, Frame.FusionChangeLeader::reader),
    FUSION_MERGE(15, Frame.FusionMerge::reader)

    ;

    public final byte value;
    private final Function<FrameReaderPart, Reader<? extends Frame>> readerConstructor;

    FrameOpcodes(int value, Function<FrameReaderPart, Reader<? extends Frame>> readerConstructor) {
        Objects.requireNonNull(readerConstructor);
        this.value = (byte) value;
        this.readerConstructor = readerConstructor;
    }

    @SuppressWarnings("unchecked")
    public Reader<Frame> reader(FrameReaderPart part) {
        Objects.requireNonNull(readerConstructor);
        return (Reader<Frame>) readerConstructor.apply(part);
    }

    private static final Map<Byte, FrameOpcodes> map;
    static {
        map = Arrays.stream(FrameOpcodes.values()).collect(Collectors.toUnmodifiableMap(e -> e.value, e -> e));
    }

    public static FrameOpcodes get(byte value) {
        var code = map.get(value);
        if (code == null) {
            throw new IllegalArgumentException("Unknown opcode: " + value);
        }
        return code;
    }
}