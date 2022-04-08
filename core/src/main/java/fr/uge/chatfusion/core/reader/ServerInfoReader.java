package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.frame.ServerInfo;
import fr.uge.chatfusion.core.reader.base.BaseReaders;
import fr.uge.chatfusion.core.reader.base.Reader;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class ServerInfoReader implements Reader<ServerInfo> {

    private enum State {
        DONE, WAITING_NAME, WAITING_ADDRESS, ERROR
    }

    private State state = State.WAITING_NAME;
    private final Reader<String> nameReader = BaseReaders.stringReader();
    private final InetSocketAddressReader addressReader = new InetSocketAddressReader();

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("Reader is already done or in error state");
        }

        if (state == State.WAITING_NAME) {
            var status = nameReader.process(buffer);
            if (status != ProcessStatus.DONE) {
                if (status == ProcessStatus.ERROR) {
                    state = State.ERROR;
                }
                return status;
            }
            state = State.WAITING_ADDRESS;
        }

        if (state == State.WAITING_ADDRESS) {
            var status = addressReader.process(buffer);
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

    @Override
    public ServerInfo get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Reader is not done");
        }
        return new ServerInfo(nameReader.get(), addressReader.get());
    }

    @Override
    public void reset() {
        state = State.WAITING_NAME;
        nameReader.reset();
        addressReader.reset();
    }
}
