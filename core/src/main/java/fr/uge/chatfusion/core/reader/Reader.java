package fr.uge.chatfusion.core.reader;

import java.nio.ByteBuffer;

/**
 * Interface for the Reader. A reader is used to read bytes from a {@link ByteBuffer} and transform them into java
 * objects. It is possible to read the bytes required to build the object in several calls. In fact, the
 * {@link #process(ByteBuffer)} method will return a {@link ProcessStatus} which indicates if the object has been fully
 * read or not.
 *
 * @param <T> the type of the object to read
 */
public interface Reader<T> {

    /**
     * Reads the given buffer in order to transform its content into an object.
     *
     * @param buffer the buffer to read
     * @return the status of the reader after reading the buffer
     * @throws IllegalStateException if the reader is in {@link ProcessStatus#DONE} or {@link ProcessStatus#ERROR} state
     */
    ProcessStatus process(ByteBuffer buffer);

    /**
     * Gets the object created by the reader.
     *
     * @return the object created by the reader
     * @throws IllegalStateException if the reader is not in {@link ProcessStatus#DONE} state or if an error occurred
     */
    T get();

    /**
     * Resets the reader to its initial state.
     */
    void reset();

    /**
     * Creates a {@link ReaderComposer} containing the reader.
     *
     * @return the {@link ReaderComposer} containing the reader
     */
    default ReaderComposer<T> compose() {
        return new ReaderComposer<>(this);
    }

    /**
     * Represents the status of the reader. after reading the given buffer.
     */
    enum ProcessStatus {
        /**
         * The reader has read enough bytes to transform them into an object.
         */
        DONE,
        /**
         * The reader has not read enough bytes to transform them into an object.
         */
        REFILL,
        /**
         * The reader has encountered an error while reading the buffer.
         */
        ERROR,
    }
}
