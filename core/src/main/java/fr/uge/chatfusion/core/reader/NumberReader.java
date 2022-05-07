package fr.uge.chatfusion.core.reader;


import fr.uge.chatfusion.core.base.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Function;

/**
 * A common class for all {@link Number} readers.
 *
 * @param <E> The type of the number to read.
 */
final class NumberReader<E extends Number> implements Reader<E> {
    private enum State {
        DONE, WAITING
    }

    private final ByteBuffer inner;
    private final Function<? super ByteBuffer, E> extractor;
    private State state = State.WAITING;
    private E value;

    public NumberReader(int size, Function<? super ByteBuffer, E> extractor) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be strictly positive");
        }
        Objects.requireNonNull(extractor);
        this.inner = ByteBuffer.allocate(size);
        this.extractor = extractor;
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE) {
            throw new IllegalStateException("Reader is already done.");
        }

        BufferUtils.transferTo(buffer, inner);
        if (inner.hasRemaining()) {
            return ProcessStatus.REFILL;
        }

        state = State.DONE;
        inner.flip();
        value = extractor.apply(inner);
        return ProcessStatus.DONE;
    }

    @Override
    public E get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Reader is not done.");
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        inner.clear();
    }
}
