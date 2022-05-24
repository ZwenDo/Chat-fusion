package fr.uge.chatfusion.core.reader;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A class containing factory methods for creating the base readers for primitive types and {@link String} type.
 * @see Reader
 */
public final class Readers {
    private Readers() {
        throw new AssertionError("No instances.");
    }

    /**
     * Creates a reader that reads an int.
     *
     * @return a reader that reads an int.
     */
    public static Reader<Integer> intReader() {
        return new NumberReader<>(Integer.BYTES, ByteBuffer::getInt);
    }

    /**
     * Creates a reader that reads a byte.
     *
     * @return a reader that reads a byte.
     */
    public static Reader<Byte> byteReader() {
        return new NumberReader<>(Byte.BYTES, ByteBuffer::get);
    }

    /**
     * Creates a reader that reads a long.
     *
     * @return a reader that reads a long.
     */
    public static Reader<Long> longReader() {
        return new NumberReader<>(Long.BYTES, ByteBuffer::getLong);
    }

    /**
     * Creates a reader that reads a {@link String}.
     *
     * @param charset the charset used to decode the string.
     * @param maxBytesLength the maximum number of bytes that can be read, independently of the encoding.
     * @return a reader that reads a {@link String}.
     * @throws IllegalArgumentException if {@code maxBytesLength} is negative or zero.
     */
    public static Reader<String> stringReader(Charset charset, int maxBytesLength) {
        Objects.requireNonNull(charset);
        if (maxBytesLength <= 0) {
            throw new IllegalArgumentException("maxBytesLength must be strictly positive.");
        }
        return new StringReader(charset, maxBytesLength);
    }

    /**
     * Creates a reader that is always in done state and just returns an object provided by a supplier.
     *
     * @param supplier The supplier that provides the object to return.
     * @param <R> The type of the object to return.
     * @return A reader that is always in done state and just returns an object provided by a supplier.
     */
    public static <R> Reader<R> directReader(Supplier<R> supplier) {
        Objects.requireNonNull(supplier);
        return new Reader<>() {

            @Override
            public ProcessStatus process(ByteBuffer buffer) {
                Objects.requireNonNull(buffer);
                return ProcessStatus.DONE;
            }

            @Override
            public R get() {
                return supplier.get();
            }

            @Override
            public void reset() {

            }
        };
    }
}
