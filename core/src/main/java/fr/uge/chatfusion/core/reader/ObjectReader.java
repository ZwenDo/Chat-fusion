package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.reader.base.Reader;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

final class ObjectReader<E> implements Reader<E> {
    private enum State {
        DONE, WAITING, ERROR
    }

    private final Function<? super BaseContent, E> constructor;
    private final Reader<?>[] readers;
    private BaseContent content = new BaseContent();
    private int index;
    private State state = State.WAITING;
    private E object;

    public ObjectReader(Function<? super BaseContent, E> constructor, Reader<?>... readers) {
        Objects.requireNonNull(constructor);
        for (var reader : readers) {
            Objects.requireNonNull(reader);
        }
        this.constructor = constructor;
        this.readers = Arrays.copyOf(readers, readers.length);
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("Reader is already done or in error state.");
        }

        while (index < readers.length) {
            var reader = readers[index];
            var status = reader.process(buffer);

            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }

            content.add(reader.get());
            reader.reset();
            index++;
        }

        try {
            object = constructor.apply(content);
        } catch (ObjectConstructionException e) {
            state = State.ERROR;
            var index = content.size();
            throw new IllegalStateException("Error while constructing object, near argument " + index + ": ", e);
        }

        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public E get() {
        if (index < readers.length) {
            throw new IllegalStateException("Reader is not done");
        }
        return object;
    }

    @Override
    public void reset() {
        index = 0;
        for (var reader : readers) {
            reader.reset();
        }
        state = State.WAITING;
        content = new BaseContent();
    }

}
