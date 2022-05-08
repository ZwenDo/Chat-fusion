package fr.uge.chatfusion.core.base;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Class providing utilities for {@link ByteBuffer}s.
 *
 * @apiNote Unless otherwise specified, buffers must be in writing
 * mode when methods are called and will be in writing mode after the call (the same goes for buffer created by the
 * methods).
 */
public final class BufferUtils {

    private BufferUtils() {
        throw new AssertionError("No instances.");
    }

    /**
     * Transfers data from one buffer to another.
     *
     * @param from the source buffer
     * @param to the destination buffer
     */
    public static void transferTo(ByteBuffer from, ByteBuffer to) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        from.flip();
        if (from.remaining() <= to.remaining()) {
            to.put(from);
        } else {
            var oldLimit = from.limit();
            from.limit(to.remaining());
            to.put(from);
            from.limit(oldLimit);
        }
        from.compact();
    }

    /**
     * Creates a copy of the given buffer, from 0 to position of the given buffer.
     *
     * @param buffer the buffer to copy
     * @return a copy of the given buffer
     */
    public static ByteBuffer copy(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);

        buffer.flip();
        var newBuffer = ByteBuffer.allocate(buffer.remaining());
        newBuffer.put(buffer);
        return newBuffer;
    }

    /**
     * Creates a buffer containing an encoded string preceded by its length.
     *
     * @param string the string to encode
     * @param charset the charset to use for encoding
     * @return a buffer containing the encoded string preceded by its length
     */
    public static ByteBuffer encodeString(String string, Charset charset) {
        Objects.requireNonNull(string);
        Objects.requireNonNull(charset);
        var str = charset.encode(string);
        var buffer = ByteBuffer.allocate(Integer.BYTES + str.remaining());
        buffer.putInt(str.remaining());
        buffer.put(str);
        return buffer;
    }

}
