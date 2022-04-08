package fr.uge.chatfusion.core.reader.base;

import java.nio.ByteBuffer;

public final class BaseReaders {
    private BaseReaders() {
        throw new AssertionError("No instances.");
    }

    public static Reader<Integer> intReader() {
        return new NumberReader<>(Integer.BYTES, ByteBuffer::getInt);
    }

    public static Reader<Byte> byteReader() {
        return new NumberReader<>(Byte.BYTES, ByteBuffer::get);
    }

    public static Reader<String> stringReader() {
        return new StringReader();
    }
}
