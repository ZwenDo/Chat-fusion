package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.frame.ServerInfo;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;

public final class FrameReader<E> implements Reader<E> {
    public enum ArgType {
        INT, STRING, ADDRESS, SERVER_INFO, LIST
    }

    private enum State {
        DONE, WAITING, ERROR
    }

    // Readers
    private final Reader<Integer> intReader = NumberReaders.intReader();
    private final StringReader stringReader = new StringReader();
    private final InetSocketAddressReader addressReader = new InetSocketAddressReader();
    private final ServerInfoReader serverInfoReader = new ServerInfoReader();

    private ListReader<?> listReader;
    private boolean readingList;

    private final Function<? super FrameContent, E> constructor;
    private final ArgType[] argTypes;
    private FrameContent content = new FrameContent();
    private int argIndex;
    private State state = State.WAITING;
    private E frame;



    public FrameReader(Function<? super FrameContent, E> constructor, ArgType... types) {
        Objects.requireNonNull(constructor);
        this.constructor = constructor;
        this.argTypes = Arrays.copyOf(types, types.length);
    }


    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("Reader is already done or in error state");
        }

        ProcessStatus status;
        while (argIndex < argTypes.length) {
            if (readingList) {
                status = read(buffer, listReader);
                if (status == ProcessStatus.DONE) readingList = false;
            } else {
                status = switch (argTypes[argIndex]) {
                    case INT -> read(buffer, intReader);
                    case STRING -> read(buffer, stringReader);
                    case ADDRESS -> read(buffer, addressReader);
                    case SERVER_INFO -> read(buffer, serverInfoReader);
                    case LIST -> readList(buffer);
                };
            }

            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }

            argIndex++;
        }

        frame = constructor.apply(content);
        return ProcessStatus.DONE;
    }


    private ProcessStatus readList(ByteBuffer buffer) {
        if (argIndex >= argTypes.length) {
            throw new IllegalStateException("List argument is missing");
        }

        var inner = (Reader<?>) switch (argTypes[argIndex + 1]) {
            case INT -> intReader;
            case STRING -> stringReader;
            case ADDRESS -> addressReader;
            case SERVER_INFO -> serverInfoReader;
            case LIST -> throw new IllegalStateException("List cannot be nested");
        };

        listReader = new ListReader<>(inner);
        readingList = true;
        return ProcessStatus.DONE;
    }

    @Override
    public E get() {
        if (argIndex < argTypes.length) {
            throw new IllegalStateException("Not done");
        }
        return frame;
    }

    @Override
    public void reset() {
        argIndex = 0;
        stringReader.reset();
        intReader.reset();
        content = new FrameContent();
    }


    private <T> ProcessStatus read(ByteBuffer buffer, Reader<T> reader) {
        var status = reader.process(buffer);
        if (status != ProcessStatus.DONE) {
            return status;
        }

        content.add(reader.get());
        reader.reset();
        return status;
    }


    static final class FrameContent {
        private final ArrayDeque<Object> deque = new ArrayDeque<>();

        private void add(Object o) {
            Objects.requireNonNull(o);
            deque.add(o);
        }

        public int nextInt() {
            return (int) deque.pop();
        }

        public String nextString() {
            return (String) deque.pop();
        }

        public InetSocketAddress nextAddress() {
            return (InetSocketAddress) deque.pop();
        }

        public ServerInfo nextServerInfo() {
            return (ServerInfo) deque.pop();
        }

        @SuppressWarnings("unchecked")
        public <T> List<T> nextList(Class<T> clazz) {
            Objects.requireNonNull(clazz);
            return (List<T>) deque.pop();
        }
    }
}
