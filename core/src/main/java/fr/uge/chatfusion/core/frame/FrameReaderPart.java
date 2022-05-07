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

    public static FrameReaderPart create(Reader<Byte> byteReader) {
        Objects.requireNonNull(byteReader);

        var intReader = Readers.intReader();
        var stringReader = Readers.stringReader(Charsets.DEFAULT_CHARSET, Sizes.MAX_MESSAGE_SIZE);
        var stringListReader = stringReader.repeat(intReader, Collectors.toUnmodifiableList());
        var addressReader = addressReader(byteReader, intReader);
        var bytesReader = byteReader.repeat(intReader, toByteBufferCollector());
        var longReader = Readers.longReader();
        return new FrameReaderPart(intReader, longReader, stringReader, stringListReader, addressReader, bytesReader);
    }

    private static Reader<InetSocketAddress> addressReader(Reader<Byte> byteReader, Reader<Integer> intReader) {
        var ctx = new Object() {
            byte[] address;
        };

        return byteReader.repeat(byteReader, toByteArrayCollector())
            .andThen(intReader, a -> ctx.address = a)
            .andFinally(p -> {
                try {
                    return new InetSocketAddress(InetAddress.getByAddress(ctx.address), p);
                } catch (UnknownHostException e) {
                    throw new UncheckedIOException(e);
                }
            });
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

    public Reader<Integer> integer() {
        return integer;
    }

    public Reader<Long> longInteger() {
        return longInteger;
    }

    public Reader<String> string() {
        return string;
    }

    public Reader<List<String>> stringList() {
        return stringList;
    }

    public Reader<InetSocketAddress> address() {
        return address;
    }

    public Reader<ByteBuffer> byteBuffer() {
        return byteBuffer;
    }
}
