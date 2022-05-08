package fr.uge.chatfusion.core.selection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Defines the class that manages {@link java.nio.channels.SocketChannel} in non-blocking mode.
 */
public interface SelectionKeyController {

    /**
     * Reads data from the channel.
     *
     * @throws IOException if an I/O error occurs
     */
    void doRead() throws IOException;

    /**
     * Writes data to the channel.
     *
     * @throws IOException if an I/O error occurs
     */
    void doWrite() throws IOException;

    /**
     * Connects the channel to the specified remote address.
     *
     * @throws IOException if an I/O error occurs
     */
    void doConnect() throws IOException;

    /**
     * Queues data to be written to the channel.
     *
     * @param data the data to be written
     */
    void queueData(ByteBuffer data);

    /**
     * Closes the channel when all data has been written to the channel.
     */
    void closeWhenAllSent();

    /**
     * Closes the channel.
     */
    void close();

    /**
     * Gets the remote address of the channel.
     *
     * @return the remote address of the channel
     */
    InetSocketAddress remoteAddress();
}
