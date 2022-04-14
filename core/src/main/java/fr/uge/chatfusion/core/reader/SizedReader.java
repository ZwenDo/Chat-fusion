package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.reader.base.Reader;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class SizedReader<T, A, R> implements Reader<R> {
    private enum State {
        DONE, WAITING_SIZE, WAITING_CONTENT, ERROR
    }

    private final Reader<? extends Number> sizeReader;
    private final Reader<T> reader;
    private final Supplier<? extends A> supplier;
    private final BiConsumer<? super A, ? super T> accumulator;
    private final Function<? super A, ? extends R> finisher;
    private A container;
    private R result;
    private int index;
    private int size;
    private State state = State.WAITING_SIZE;

    public SizedReader(
        Reader<? extends Number> sizeReader,
        Reader<T> reader,
        Supplier<? extends A> supplier,
        BiConsumer<? super A, ? super T> accumulator,
        Function<? super A, ? extends R> finisher
    ) {
        Objects.requireNonNull(sizeReader);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(finisher);
        this.sizeReader = sizeReader;
        this.reader = reader;
        this.accumulator = accumulator;
        this.supplier = supplier;
        this.finisher = finisher;
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
            container = supplier.get();
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

        result = finisher.apply(container);
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

            accumulator.accept(container, reader.get());
            reader.reset();
            index++;
        }
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public R get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Reader is not done");
        }
        return result;
    }

    @Override
    public void reset() {
        sizeReader.reset();
        reader.reset();
        state = State.WAITING_SIZE;
    }

    void setSize(int size) {
        this.size = size;
    }
}
