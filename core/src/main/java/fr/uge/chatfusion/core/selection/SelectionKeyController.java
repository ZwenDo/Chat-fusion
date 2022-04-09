package fr.uge.chatfusion.core.selection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface SelectionKeyController {

    void doRead() throws IOException;

    void doWrite() throws IOException;

    void doConnect() throws IOException;

    void queueData(ByteBuffer data);

    void closeWhenAllSent();

    void close();

    InetSocketAddress remoteAddress();
}
