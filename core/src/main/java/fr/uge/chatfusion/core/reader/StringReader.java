package fr.uge.chatfusion.core.reader;


import fr.uge.chatfusion.core.base.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * A reader that reads a {@link String}.
 */
final class StringReader implements Reader<String> {
    private enum State {
        DONE, WAITING_SIZE, WAITING_TEXT, ERROR
    }

    private final Reader<Integer> sizeReader = Readers.intReader();
    private final Charset charset;
    private final ByteBuffer textBuffer;
    private State state = State.WAITING_SIZE;
    private String text;

    public StringReader(Charset charset, int maxTextLength) {
        Objects.requireNonNull(charset);
        if (maxTextLength < 0) {
            throw new IllegalArgumentException("maxTextLength must be positive.");
        }
        this.charset = charset;
        textBuffer = ByteBuffer.allocate(maxTextLength);
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("Reader is already done or in error state.");
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

        if (state == State.WAITING_TEXT) {
            computeText(buffer);
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
            text = charset.decode(textBuffer).toString();
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
