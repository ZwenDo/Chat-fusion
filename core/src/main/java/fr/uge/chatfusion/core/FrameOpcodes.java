package fr.uge.chatfusion.core;

public final class FrameOpcodes {
    private FrameOpcodes() {
        throw new AssertionError("No instances.");
    }

    public static final byte ANONYMOUS_LOGIN = 0;
    public static final byte LOGIN_ACCEPTED = 2;
    public static final byte LOGIN_REFUSED = 3;
    public static final byte PUBLIC_MESSAGE = 4;
    public static final byte FUSION_INIT = 8;
    public static final byte FUSION_INIT_FWD = 11;
    public static final byte FUSION_INIT_OK = 9;
    public static final byte FUSION_INIT_KO = 10;
    public static final byte FUSION_REQUEST = 12;
    public static final byte FUSION_CHANGE_LEADER = 14;
    public static final byte FUSION_MERGE = 15;
}
