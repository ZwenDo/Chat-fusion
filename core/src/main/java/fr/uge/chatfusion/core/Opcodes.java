package fr.uge.chatfusion.core;

public enum Opcodes {
    ANONYMOUS_LOGIN {
        @Override
        public byte value() {
            return 0;
        }
    },
    PUBLIC_MESSAGE {
        @Override
        public byte value() {
            return 4;
        }
    }
    ;

    public abstract byte value();
}
