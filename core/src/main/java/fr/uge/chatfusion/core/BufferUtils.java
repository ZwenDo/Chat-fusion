package fr.uge.chatfusion.core;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class BufferUtils {

    private BufferUtils() {
        throw new AssertionError("No instances.");
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
