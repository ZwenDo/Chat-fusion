package fr.uge.chatfusion.core.frame;

import fr.uge.chatfusion.core.base.BufferUtils;
import fr.uge.chatfusion.core.base.Charsets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * A builder of {@link ByteBuffer}s containing the data of a {@link Frame}.
 */
final class FrameBuilder {
    private ByteBuffer buffer = ByteBuffer.allocate(1_024);

    /**
     * Constructor.
     *
     * @param opcode the opcode of the frame
     */
    public FrameBuilder(FrameOpcode opcode) {
        buffer.put(opcode.value());
    }

    /**
     * Adds an integer to the buffer
     *
     * @param i the integer to add
     * @return this
     */
    public FrameBuilder addInt(int i) {
        if (Integer.BYTES > buffer.remaining()) {
            grow();
        }
        buffer.putInt(i);
        return this;
    }

    /**
     * Adds a {@link String} to the buffer.
     *
     * @apiNote The string is encoded using the {@link Charsets#DEFAULT_CHARSET} charset and is prefixed with its
     * length in this encoding.
     *
     * @param string the string to add
     * @return this
     */
    public FrameBuilder addString(String string) {
        Objects.requireNonNull(string);
        var bb = BufferUtils.encodeString(string, Charsets.DEFAULT_CHARSET);
        bb.flip();
        if (bb.remaining() > buffer.remaining()) {
            grow();
        }
        buffer.put(bb);
        return this;
    }

    /**
     * Adds a {@link InetSocketAddress} to the buffer.
     *
     * @param address the address to add
     * @return this
     */
    public FrameBuilder addAddress(InetSocketAddress address) {
        Objects.requireNonNull(address);
        var inet = address.getAddress().getAddress();
        if (inet.length != 4 && inet.length != 16) {
            throw new AssertionError("Impossible address length");
        }
        if (Byte.BYTES + inet.length + Integer.BYTES > buffer.remaining()) {
            grow();
        }
        buffer.put((byte) inet.length);
        buffer.put(inet);
        buffer.putInt(address.getPort());
        return this;
    }

    /**
     * Adds a list of {@link String} to the buffer.
     *
     * @param strings the list of strings to add
     * @return this
     */
    public FrameBuilder addStringList(List<String> strings) {
        Objects.requireNonNull(strings);
        if (Integer.BYTES > buffer.remaining()) {
            grow();
        }
        buffer.putInt(strings.size());
        strings.forEach(this::addString);
        return this;
    }

    /**
     * Adds a {@link ByteBuffer} to the buffer.
     *
     * @apiNote The given buffer content is consumed meaning that after the call its position will be equal to its
     * limit.
     *
     * @param buffer the buffer to add
     * @return this
     */
    public FrameBuilder addBuffer(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        while (buffer.position() > this.buffer.remaining()) {
            grow();
        }
        buffer.flip();
        this.buffer.putInt(buffer.remaining());
        this.buffer.put(buffer);
        buffer.compact();
        return this;
    }

    /**
     * Adds a long to the buffer.
     *
     * @param l the long to add
     * @return this
     */
    public FrameBuilder addLong(long l) {
        if (Long.BYTES > buffer.remaining()) {
            grow();
        }
        buffer.putLong(l);
        return this;
    }

    /**
     * Creates a {@link Frame} from the content of this builder.
     *
     * @return the created buffer
     */
    public ByteBuffer build() {
        return BufferUtils.copy(buffer);
    }

    private void grow() {
        var bigger = ByteBuffer.allocate(buffer.capacity() * 2);
        buffer.flip();
        bigger.put(buffer);
        buffer = bigger;
    }
}
