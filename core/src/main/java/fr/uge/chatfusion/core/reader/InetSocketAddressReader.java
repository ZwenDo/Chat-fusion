package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.BufferUtils;
import fr.uge.chatfusion.core.reader.base.BaseReaders;
import fr.uge.chatfusion.core.reader.base.Reader;

import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Objects;

public final class InetSocketAddressReader implements Reader<InetSocketAddress> {
    private enum State {
        DONE, WAITING_KIND, WAITING_IP, WAITING_PORT, ERROR
    }

    private final Reader<Byte> ipKindReader = BaseReaders.byteReader();
    private final Reader<Integer> portReader = BaseReaders.intReader();
    private ByteBuffer ipBuffer;
    private State state = State.WAITING_KIND;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("Reader is already done or in error state");
        }

        if (state == State.WAITING_KIND) {
            var status = computeKind(buffer);
            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }
        }

        if (state == State.WAITING_IP) {
            BufferUtils.transferTo(buffer, ipBuffer);
            if (ipBuffer.hasRemaining()) {
                return ProcessStatus.REFILL;
            }
            state = State.WAITING_PORT;
        }


        if (state == State.WAITING_PORT) {
            var status = portReader.process(buffer);
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

    private ProcessStatus computeKind(ByteBuffer buffer) {
        var status = ipKindReader.process(buffer);
        if (status != ProcessStatus.DONE) {
            return status;
        }

        var kind = ipKindReader.get();
        if (kind != 4 && kind != 6) {
            state = State.ERROR;
            return ProcessStatus.ERROR;
        }

        state = State.WAITING_IP;
        ipBuffer = kind == 4 ? ByteBuffer.allocate(4) : ByteBuffer.allocate(16);
        return ProcessStatus.DONE;
    }

    @Override
    public InetSocketAddress get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Reader is not done");
        }
        try {
            return new InetSocketAddress(InetAddress.getByAddress(ipBuffer.array()), portReader.get());
        } catch (UnknownHostException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void reset() {
        state = State.WAITING_KIND;
        ipKindReader.reset();
        portReader.reset();
    }
}
