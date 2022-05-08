package fr.uge.chatfusion.core.frame;

import fr.uge.chatfusion.core.base.Charsets;
import fr.uge.chatfusion.core.base.Sizes;
import fr.uge.chatfusion.core.reader.Reader;
import fr.uge.chatfusion.core.reader.Readers;

import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Class used to create a frame reader. This class is simply a bean that stores the different basic readers used
 * to create the frame readers without instantiating them several times.
 */
final class FrameReaderPart {
    private final Reader<Integer> integer;
    private final Reader<Long> longInteger;
    private final Reader<String> string;
    private final Reader<List<String>> stringList;
    private final Reader<InetSocketAddress> address;
    private final Reader<ByteBuffer> byteBuffer;

    private FrameReaderPart(
        Reader<Integer> integer,
        Reader<Long> longInteger,
        Reader<String> string,
        Reader<List<String>> stringList,
        Reader<InetSocketAddress> address,
        Reader<ByteBuffer> byteBuffer
    ) {
        this.integer = integer;
        this.longInteger = longInteger;
        this.string = string;
        this.stringList = stringList;
        this.address = address;
        this.byteBuffer = byteBuffer;
    }

    /**
     * Creates a frame reader part.
     *
     * @param byteReader the byte reader used in the frame reader part
     * @return a frame reader part
     */
    public static FrameReaderPart create(Reader<Byte> byteReader) {
        Objects.requireNonNull(byteReader);

        var intReader = Readers.intReader();
        var stringReader = Readers.stringReader(Charsets.DEFAULT_CHARSET, Sizes.MAX_MESSAGE_SIZE);
        var stringListReader = stringReader.compose()
            .repeat(intReader, Collectors.toUnmodifiableList())
            .toReader();
        var addressReader = addressReader(byteReader, intReader);
        var bytesReader = byteReader.compose()
            .repeat(intReader, toByteBufferCollector())
            .toReader();
        var longReader = Readers.longReader();
        return new FrameReaderPart(intReader, longReader, stringReader, stringListReader, addressReader, bytesReader);
    }

    private static Reader<InetSocketAddress> addressReader(Reader<Byte> byteReader, Reader<Integer> intReader) {
        var ctx = new Object() {
            byte[] address;
        };

        return byteReader.compose()
            .repeat(byteReader, toByteArrayCollector())
            .andThen(intReader, a -> ctx.address = a)
            .andFinally(p -> {
                try {
                    return new InetSocketAddress(InetAddress.getByAddress(ctx.address), p);
                } catch (UnknownHostException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .toReader();
    }

    private static Collector<Byte, List<Byte>, ByteBuffer> toByteBufferCollector() {
        return Collector.of(
            ArrayList::new,
            List::add,
            (l, r) -> {
                l.addAll(r);
                return l;
            },
            l -> ByteBuffer.wrap(listToByteArray(l)).compact()
        );
    }

    private static Collector<Byte, List<Byte>, byte[]> toByteArrayCollector() {
        return Collector.of(
            ArrayList::new,
            List::add,
            (l, r) -> {
                l.addAll(r);
                return l;
            },
            FrameReaderPart::listToByteArray
        );
    }

    private static byte[] listToByteArray(List<Byte> l) {
        var size = l.size();
        var bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = l.get(i);
        }
        return bytes;
    }

    /**
     * Gets the integer reader.
     *
     * @return the integer reader
     */
    public Reader<Integer> integer() {
        return integer;
    }

    /**
     * Gets the long integer reader.
     *
     * @return the long integer reader
     */
    public Reader<Long> longInteger() {
        return longInteger;
    }

    /**
     * Gets the string reader.
     *
     * @return the string reader
     */
    public Reader<String> string() {
        return string;
    }

    /**
     * Gets the string list reader.
     *
     * @return the string list reader
     */
    public Reader<List<String>> stringList() {
        return stringList;
    }

    /**
     * Gets the address reader.
     *
     * @return the address reader
     */
    public Reader<InetSocketAddress> address() {
        return address;
    }

    /**
     * Gets the byte buffer reader.
     *
     * @return the byte buffer reader
     */
    public Reader<ByteBuffer> byteBuffer() {
        return byteBuffer;
    }
}
