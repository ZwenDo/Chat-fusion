package fr.uge.chatfusion.core.frame;

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
        currentReader.reset();
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
        state = State.WAITING_DATA;
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
    }

    private static Function<Byte, Reader<Frame>> opcodeToReader(Reader<Byte> byteReader) {
        var parts = parts(byteReader);
        var readers = Arrays.stream(FrameOpcodes.values())
            .map(o -> o.reader(parts))
            .<Reader<Frame>>toArray(Reader[]::new);

        return b -> {
            try {
                return readers[FrameOpcodes.get(b).ordinal()];
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unknown opcode: " + b);
            }
        };
    }

    private static FrameReaderPart parts(Reader<Byte> byteReader) {
        var intReader = BaseReaders.intReader();
        var stringReader = BaseReaders.stringReader();
        var stringListReader = Readers.<List<String>, String>sizedReader(
            stringReader,
            ArrayList::new,
            (i, v, l) -> {
                l.add(v);
                return l;
            },
            List::copyOf
        );
        var byteArrayReader = Readers.sizedReader(
            byteReader,
            byte[]::new,
            (i, v, a) -> {
                a[i] = v;
                return a;
            },
            (a) -> Arrays.copyOf(a, a.length)
        );
        var addressReader = Readers.objectReader(
            c -> {
                try {
                    return new InetSocketAddress(InetAddress.getByAddress(c.next()), c.next());
                } catch (UnknownHostException e) {
                    throw new UncheckedIOException(e);
                }
            },
            byteArrayReader,
            intReader
        );
        return new FrameReaderPart(intReader, stringReader, stringListReader, byteArrayReader, addressReader);
    }
}
