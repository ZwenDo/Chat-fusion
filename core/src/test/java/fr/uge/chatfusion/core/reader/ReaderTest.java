package fr.uge.chatfusion.core.reader;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ReaderTest {

    @Test
    public void primitiveReaderSimpleTest() {
        var reader = Readers.intReader();
        var buffer = ByteBuffer.allocate(Integer.BYTES);
        var value = 5;
        buffer.putInt(value);
        var status = reader.process(buffer);

        assertEquals(Reader.ProcessStatus.DONE, status);
        assertEquals(value, reader.get());
        assertEquals(buffer.capacity(), buffer.remaining());
        assertThrows(IllegalStateException.class, () -> reader.process(buffer));
        reader.reset();
        status = reader.process(buffer);
        assertEquals(Reader.ProcessStatus.REFILL, status);
    }

    @Test
    public void stringReaderSimpleTest() {
        var cs = StandardCharsets.UTF_8;
        var string = "test";
        var bytes = string.getBytes(cs);
        var buffer = ByteBuffer.allocate(Integer.BYTES + bytes.length);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        var reader = Readers.stringReader(cs, bytes.length);
        var status = reader.process(buffer);

        assertEquals(Reader.ProcessStatus.DONE, status);
        assertEquals(string, reader.get());
        assertEquals(buffer.capacity(), buffer.remaining());
        assertThrows(IllegalStateException.class, () -> reader.process(buffer));
        reader.reset();
        status = reader.process(buffer);
        assertEquals(Reader.ProcessStatus.REFILL, status);
    }

    @Test
    public void stringReaderStringTooLongTest() {
        var cs = StandardCharsets.UTF_8;
        var string = "test";
        var bytes = string.getBytes(cs);
        var buffer = ByteBuffer.allocate(Integer.BYTES + bytes.length);
        buffer.putInt(bytes.length);
        buffer.put(bytes);

        var reader = Readers.stringReader(cs, bytes.length - 1);
        var status = reader.process(buffer);

        assertEquals(Reader.ProcessStatus.ERROR, status);
        assertThrows(IllegalStateException.class, reader::get);
    }

}
