package fr.uge.chatfusion.core.reader;


import fr.uge.chatfusion.core.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class StringReader implements Reader<String> {
    private static final Charset CS = StandardCharsets.UTF_8;
    private static final int MAX_BUFFER_SIZE = 1024;

    private enum State {
        DONE, WAITING_SIZE, WAITING_TEXT, ERROR
    }

    private State state = State.WAITING_SIZE;
    private final ByteBuffer textBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
    private final Reader<Integer> sizeReader = NumberReaders.intReader();
    private String text;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("Reader is already done or in error state");
        }

        if (state == State.WAITING_SIZE) {
            var status = computeSize(buffer);
            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }
        }

        buffer.flip();
        try {
            if (state == State.WAITING_TEXT) {
                computeText(buffer);
            }
        } finally {
            buffer.compact();
        }

        if (text == null) return ProcessStatus.REFILL;
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    private ProcessStatus computeSize(ByteBuffer buffer) {
        var status = sizeReader.process(buffer);
        if (status != ProcessStatus.DONE) {
            return status;
        }

        var size = sizeReader.get();
        if (size > textBuffer.capacity() || size < 0) {
            return ProcessStatus.ERROR;
        }

        state = State.WAITING_TEXT;
        textBuffer.limit(sizeReader.get());

        return ProcessStatus.DONE;
    }

    private void computeText(ByteBuffer buffer) {
        BufferUtils.transferTo(buffer, textBuffer);
        if (!textBuffer.hasRemaining()) {
            textBuffer.flip();
            text = CS.decode(textBuffer).toString();
            textBuffer.compact();
        }
    }

    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Reader is not done");
        }
        return text;
    }

    @Override
    public void reset() {
        state = State.WAITING_SIZE;
        text = null;
        textBuffer.clear();
        sizeReader.reset();
    }
}
