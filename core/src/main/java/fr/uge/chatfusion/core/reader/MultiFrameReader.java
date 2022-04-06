package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.frame.Frame;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class MultiFrameReader implements Reader<Frame> {

    private enum State {
        DONE, WAITING_OPCODE, WAITING_DATA, ERROR
    }

    private final Map<Byte, Reader<? extends Frame>> readers;
    private final Reader<Byte> opcodeReader = NumberReaders.byteReader();
    private State state = State.WAITING_OPCODE;
    private Reader<? extends Frame> currentReader;

    public MultiFrameReader(Supplier<Map<Byte, Reader<? extends Frame>>> factory) {
        Objects.requireNonNull(factory);
        readers = factory.get();
    }

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

        state = State.DONE;
        return ProcessStatus.DONE;
    }

    private ProcessStatus setCurrentReader(ByteBuffer buffer) {
        var status = opcodeReader.process(buffer);
        if (status != ProcessStatus.DONE) {
            return status;
        }

        currentReader = readers.get(opcodeReader.get());
        if (currentReader == null) {
            state = State.ERROR;
            throw new IllegalStateException("No reader for opcode " + opcodeReader.get());
        }

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
        readers.values().forEach(Reader::reset);
        state = State.WAITING_OPCODE;
    }
}
