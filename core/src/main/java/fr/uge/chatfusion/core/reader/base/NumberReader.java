package fr.uge.chatfusion.core.reader.base;

import fr.uge.chatfusion.core.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Function;

final class NumberReader<E extends Number> implements Reader<E> {
    private enum State {
        DONE, WAITING
    }

    private State state = State.WAITING;

    private final ByteBuffer internalBuffer;
    private final Function<? super ByteBuffer, E> extractor;
    private E value;

    public NumberReader(int size, Function<? super ByteBuffer, E> extractor) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be strictly positive");
        }
        Objects.requireNonNull(extractor);
        this.internalBuffer = ByteBuffer.allocate(size);
        this.extractor = extractor;
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE) {
            throw new IllegalStateException("Already done.");
        }

        BufferUtils.transferTo(buffer, internalBuffer);

        if (internalBuffer.hasRemaining()) {
            return ProcessStatus.REFILL;
        }
        state = State.DONE;
        internalBuffer.flip();
        value = extractor.apply(internalBuffer);
        return ProcessStatus.DONE;
    }

    @Override
    public E get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Not done.");
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        internalBuffer.clear();
    }
}
