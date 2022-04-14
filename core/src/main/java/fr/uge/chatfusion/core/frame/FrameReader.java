package fr.uge.chatfusion.core.frame;

import fr.uge.chatfusion.core.Sizes;
import fr.uge.chatfusion.core.reader.Readers;
import fr.uge.chatfusion.core.reader.base.BaseReaders;
import fr.uge.chatfusion.core.reader.base.Reader;

import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

final class FrameReader implements Reader<Frame> {

    private enum State {
        DONE, WAITING_OPCODE, WAITING_DATA, ERROR
    }

    private final Reader<Byte> opcodeReader = BaseReaders.byteReader();
    private final Function<Byte, Reader<Frame>> readers = opcodeToReader(opcodeReader);
    private Reader<? extends Frame> currentReader;
    private State state = State.WAITING_OPCODE;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("Reader is already done or in error state");
        }

        if (state == State.WAITING_OPCODE) {
            var status = setCurrentReader(buffer);
            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }
            state = State.WAITING_DATA;
        }

        if (state == State.WAITING_DATA) {
            var status = currentReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }
        }

        state = State.DONE;
        return ProcessStatus.DONE;
    }

    private ProcessStatus setCurrentReader(ByteBuffer buffer) {
        var status = opcodeReader.process(buffer);
        if (status != ProcessStatus.DONE) {
            return status;
        }

        currentReader = readers.apply(opcodeReader.get());
        opcodeReader.reset();
        return ProcessStatus.DONE;
    }

    @Override
    public Frame get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Reader is not done");
        }
        return currentReader.get();
    }

    @Override
    public void reset() {
        state = State.WAITING_OPCODE;
        currentReader.reset();
    }

    private static Function<Byte, Reader<Frame>> opcodeToReader(Reader<Byte> byteReader) {
        var parts = parts(byteReader);
        @SuppressWarnings("unchecked")
        var readers = (Reader<Frame>[]) Arrays.stream(FrameOpcode.values())
            .map(op -> op.reader(parts))
            .toArray(Reader[]::new);

        return b -> {
            try {
                return readers[FrameOpcode.get(b).ordinal()];
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unknown opcode: " + b);
            }
        };
    }

    private static FrameReaderPart parts(Reader<Byte> byteReader) {
        var intReader = BaseReaders.intReader();
        var stringReader = BaseReaders.stringReader(Sizes.MAX_MESSAGE_SIZE);
        var stringListReader = Readers.sizedReader(
            intReader,
            stringReader,
            Collectors.toUnmodifiableList()
        );
        var addressReader = inetSocketAddressReader(byteReader, intReader);
        var bytesReader = Readers.<Byte, List<Byte>, ByteBuffer>sizedReader(
            intReader,
            byteReader,
            ArrayList::new,
            List::add,
            l -> {
                var size = l.size();
                var bytes = new byte[size];
                for (int i = 0; i < size; i++) {
                    bytes[i] = l.get(i);
                }
                return ByteBuffer.wrap(bytes).compact();
            }
        );
        var longReader = BaseReaders.longReader();
        return new FrameReaderPart(intReader, longReader, stringReader, stringListReader, addressReader, bytesReader);
    }

    private static Reader<InetSocketAddress> inetSocketAddressReader(
        Reader<Byte> byteReader,
        Reader<Integer> intReader
    ) {
        var addressArrayReader = Readers.<Byte, List<Byte>, byte[]>sizedReader(
            byteReader,
            byteReader,
            ArrayList::new,
            List::add,
            l -> {
                var size = l.size();
                var bytes = new byte[size];
                for (int i = 0; i < size; i++) {
                    bytes[i] = l.get(i);
                }
                return bytes;
            }
        );
        return Readers.objectReader(
            c -> {
                try {
                    return new InetSocketAddress(InetAddress.getByAddress(c.next()), c.next());
                } catch (UnknownHostException e) {
                    throw new UncheckedIOException(e);
                }
            },
            addressArrayReader,
            intReader
        );
    }

    record FrameReaderPart(
        Reader<Integer> integer,
        Reader<Long> longInteger,
        Reader<String> string,
        Reader<List<String>> stringList,
        Reader<InetSocketAddress> address,
        Reader<ByteBuffer> byteBuffer
    ) {
        public FrameReaderPart {
            Objects.requireNonNull(integer);
            Objects.requireNonNull(longInteger);
            Objects.requireNonNull(string);
            Objects.requireNonNull(stringList);
            Objects.requireNonNull(address);
            Objects.requireNonNull(byteBuffer);
        }
    }
}
