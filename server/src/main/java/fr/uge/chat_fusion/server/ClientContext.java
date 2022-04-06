package fr.uge.chat_fusion.server;

import fr.uge.chat_fusion.core.BufferUtils;
import fr.uge.chat_fusion.core.SilentlyCloseable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ClientContext implements SilentlyCloseable {
    private static final int BUFFER_SIZE = 1_024;
    private static final Logger LOGGER = Logger.getLogger(ClientContext.class.getName());

    private final SelectionKey key;
    private final SocketChannel sc;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private final ArrayDeque<ByteBuffer> queue = new ArrayDeque<>();
    private boolean closed = false;

    public ClientContext(SelectionKey key) {
        Objects.requireNonNull(key);
        this.key = key;
        this.sc = (SocketChannel) key.channel();
    }

    private void updateInterestOps() {

    }

    private void processIn() {
        while (bufferIn.hasRemaining()) {
           /* switch (reader.process(bufferIn)) {
                case DONE -> {
                    broadcast(reader.get());
                    reader.reset();
                }
                case ERROR -> {
                    logger.log(Level.SEVERE, "Malformed message packet");
                    silentlyClose();
                    return;
                }
                case REFILL -> {
                    return;
                }
            }*/
        }
    }

    /**
     * Try to fill bufferOut from the message queue
     *
     */
    private void processOut() {
        while (!queue.isEmpty() && bufferOut.hasRemaining()) {
            var msg = queue.peekFirst();
            if (!msg.hasRemaining()) {
                queue.removeFirst();
                continue;
            }
            BufferUtils.transferTo(msg, bufferOut);
        }
    }

    /**
     * Performs the read action on sc
     *
     * The convention is that both buffers are in write-mode before the call to
     * doRead and after the call
     *
     * @throws IOException
     */
    private void doRead() throws IOException {
        var bytes = sc.read(bufferIn);

        if (bytes == 0) {
            LOGGER.warning("Selector lied!");
            return;
        }

        if (bytes == -1) closed = true;

        processIn();
        updateInterestOps();
    }

    private void doWrite() throws IOException {
        bufferOut.flip();

        var bytes = sc.write(bufferOut);
        bufferOut.compact();

        if (bytes == 0) {
            LOGGER.warning("Selector lied!");
        }

        processOut();
        updateInterestOps();
    }

    @Override
    public void close() throws IOException {
        sc.close();
    }
}
