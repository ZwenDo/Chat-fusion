package fr.uge.chat_fusion.core.reader;

import java.nio.ByteBuffer;

public interface Reader<T> {

    enum ProcessStatus { DONE, REFILL, ERROR }

    ProcessStatus process(ByteBuffer bb);

    T get();

    void reset();

}
