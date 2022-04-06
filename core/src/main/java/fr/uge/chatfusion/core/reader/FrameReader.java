package fr.uge.chatfusion.core.reader;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class FrameReader<E> implements Reader<E> {
    public enum ArgType {
        INT, STRING
    }
    private enum State {
        DONE, WAITING, ERROR
    }

    private final Reader<Integer> intReader = NumberReaders.intReader();
    private final StringReader stringReader = new StringReader();
    private final Function<? super FrameContent, E> constructor;
    private final ArgType[] argTypes;
    private FrameContent content = new FrameContent();
    private int argIndex;
    private State state = State.WAITING;

    public FrameReader(Function<? super FrameContent, E> constructor, ArgType... types) {
        Objects.requireNonNull(constructor);
        if (types.length < 1) {
            throw new IllegalArgumentException("At least one argument type is required");
        }
        this.constructor = constructor;
        this.argTypes = Arrays.copyOf(types, types.length);
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("Reader is already done or in error state");
        }

        var status = switch (argTypes[argIndex]) {
            case INT -> readInt(buffer);
            case STRING -> readString(buffer);
        };

        if (status != ProcessStatus.DONE) {
            if (status == ProcessStatus.ERROR) {
                state = State.ERROR;
            }
            return status;
        }

        return argIndex < argTypes.length ? ProcessStatus.REFILL : ProcessStatus.DONE;
    }

    @Override
    public E get() {
        if (argIndex < argTypes.length) {
            throw new IllegalStateException("Not done");
        }
        return constructor.apply(content);
    }

    @Override
    public void reset() {
        argIndex = 0;
        stringReader.reset();
        intReader.reset();
        content = new FrameContent();
    }

    private ProcessStatus readInt(ByteBuffer buffer) {
        return read(buffer, intReader, content::addInt);
    }

    private ProcessStatus readString(ByteBuffer buffer) {
        return read(buffer, stringReader, content::addString);
    }

    private <T> ProcessStatus read(ByteBuffer buffer, Reader<T> reader, Consumer<T> action) {
        var status = reader.process(buffer);
        if (status != ProcessStatus.DONE) {
            return status;
        }
        action.accept(reader.get());
        reader.reset();
        argIndex++;
        return status;
    }

    public static final class FrameContent {
        private final ArrayDeque<String> strings = new ArrayDeque<>();
        private final ArrayDeque<Integer> ints = new ArrayDeque<>();

        private void addString(String s) {
            Objects.requireNonNull(s);
            strings.add(s);
        }

        private void addInt(int i) {
            ints.add(i);
        }
        
        public int nextInt() {
            return ints.pop();
        }

        public String nextString() {
            return strings.pop();
        }
        
        public int intsCount() {
            return ints.size();
        }

        public int stringsCount() {
            return strings.size();
        }
    }
}
