package fr.uge.chat_fusion.core.reader;

import fr.uge.chat_fusion.core.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class StringReader implements Reader<String> {
    private static final Charset CS = StandardCharsets.UTF_8;

    private enum State {
        DONE, WAITING_SIZE, WAITING_TEXT, ERROR
    }

    private State state = State.WAITING_SIZE;
    private final ByteBuffer textBuffer = ByteBuffer.allocate(1_024);
    private final IntReader sizeReader = new IntReader();
    private String text;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_SIZE) {
            if (!computeSize(buffer)) return ProcessStatus.ERROR;
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

    private boolean computeSize(ByteBuffer buffer) {
        var status = sizeReader.process(buffer);
        if (status == ProcessStatus.ERROR) {
            throw new IllegalArgumentException();
        }

        if (status == ProcessStatus.DONE) {
            state = State.WAITING_TEXT;
            var size = sizeReader.get();
            if (size > textBuffer.capacity() || size < 0) {
                return false;
            }
            textBuffer.limit(sizeReader.get());
        }
        return true;
    }


    private void computeText(ByteBuffer buffer) {
        BufferUtils.fillBuffer(buffer, textBuffer);
        if (!textBuffer.hasRemaining()) {
            textBuffer.flip();
            text = CS.decode(textBuffer).toString();
            textBuffer.compact();
        }
    }



    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
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
