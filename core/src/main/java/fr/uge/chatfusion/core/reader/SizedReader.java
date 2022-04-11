package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.reader.base.BaseReaders;
import fr.uge.chatfusion.core.reader.base.Reader;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Function;

final class SizedReader<E, R> implements Reader<E> {
    private enum State {
        DONE, WAITING_SIZE, WAITING_CONTENT, ERROR
    }

    public interface Function3<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    private final Reader<? extends Number> sizeReader;
    private final Reader<R> reader;
    private final Function<Integer, ? extends E> factory;
    private final Function3<Integer, ? super R, ? super E, ? extends E> accumulator;
    private final Function<? super E, ? extends E> copier;
    private E result;
    private int index;
    private int size;
    private State state = State.WAITING_SIZE;

    public SizedReader(
        Reader<? extends Number> sizeReader,
        Reader<R> reader,
        Function<Integer, ? extends E> factory,
        Function3<Integer, ? super R, ? super E, ? extends E> accumulator,
        Function<? super E, ? extends E> copier
    ) {
        Objects.requireNonNull(sizeReader);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(factory);
        Objects.requireNonNull(copier);
        this.sizeReader = sizeReader;
        this.reader = reader;
        this.accumulator = accumulator;
        this.factory = factory;
        this.copier = copier;
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("Reader is already done or in error state.");
        }

        if (state == State.WAITING_SIZE) {
            var status = sizeReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }
            state = State.WAITING_CONTENT;
            size = sizeReader.get().intValue();
            sizeReader.reset();
            result = factory.apply(size);
            index = 0;
        }

        if (state == State.WAITING_CONTENT) {
            var status = accumulate(buffer);
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

    private ProcessStatus accumulate(ByteBuffer buffer) {
        while (index < size) {
            var status = reader.process(buffer);
            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }

            result = accumulator.apply(index, reader.get(), result);
            reader.reset();
            index++;
        }
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public E get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Reader is not done");
        }
        return copier.apply(result);
    }

    @Override
    public void reset() {
        sizeReader.reset();
        reader.reset();
        state = State.WAITING_SIZE;
    }
}
