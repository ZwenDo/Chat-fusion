package fr.uge.chatfusion.core.reader;

import java.nio.ByteBuffer;

public final class NumberReaders {
    private NumberReaders() {
        throw new AssertionError("No instances.");
    }

    public static Reader<Integer> intReader() {
        return new NumberReader<>(Integer.BYTES, ByteBuffer::getInt);
    }

    public static Reader<Byte> byteReader() {
        return new NumberReader<>(Byte.BYTES, ByteBuffer::get);
    }
}
