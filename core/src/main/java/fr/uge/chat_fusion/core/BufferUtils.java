package fr.uge.chat_fusion.core;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class BufferUtils {

    private BufferUtils() {
        throw new AssertionError("Cannot instantiate BufferUtils.");
    }

    public static void transferTo(ByteBuffer from, ByteBuffer to) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (from.remaining() <= to.remaining()) {
            to.put(from);
        } else {
            var oldLimit = from.limit();
            from.limit(to.remaining());
            to.put(from);
            from.limit(oldLimit);
        }
    }

}
