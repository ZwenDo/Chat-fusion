package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.reader.base.BaseReaders;
import fr.uge.chatfusion.core.reader.base.Reader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ListReader<E> implements Reader<List<E>> {
    private enum State {
        DONE, WAITING_SIZE, WAITING_LIST, ERROR
    }

    private final Reader<Integer> sizeReader = BaseReaders.intReader();
    private final Reader<E> reader;
    private List<E> list = new ArrayList<>();
    private int size;
    private State state = State.WAITING_SIZE;

    public ListReader(Reader<E> reader) {
        Objects.requireNonNull(reader);
        this.reader = reader;
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);

        if (state == State.WAITING_SIZE) {
            var status = sizeReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }
            state = State.WAITING_LIST;
            size = sizeReader.get();
        }

        if (state == State.WAITING_LIST) {
            var status = fillList(buffer);
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

    private ProcessStatus fillList(ByteBuffer buffer) {
        while (size > 0) {
            var status = reader.process(buffer);
            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }

            list.add(reader.get());
            reader.reset();
            size--;
        }
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public List<E> get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Reader is not done");
        }
        return List.copyOf(list);
    }

    @Override
    public void reset() {
        sizeReader.reset();
        reader.reset();
        list = new ArrayList<>();
        state = State.WAITING_SIZE;
    }
}
