package fr.uge.chatfusion.core.reader;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * Interface for the Reader. A reader is used to read bytes from a {@link ByteBuffer} and transform them into java
 * objects. The particularity of this interface is that it is possible to read the bytes required to build the object
 * in several calls. In fact, the {@link #process(ByteBuffer)} method will return a {@link ProcessStatus} which
 * indicates if the object has been fully read or not.
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
     * Reads a {@code T} type object and maps it into and {@code R} type reader. An example of usage is a reader that
     * reads a float or a double depending on the previous byte read (for the example 0 is a float, and a different
     * value is a double).
     * <blockquote><pre>
     * public static Reader&lt;Number&gt; pointReader() {
     *      return Readers.byteReader()
     *          .map(b -> b == 0
     *              ? Readers.doubleReader()
     *              : Readers.floatReader()
     *          );
     * }
     * </pre></blockquote>
     *
     * @param mapper the function that transforms an object of type {@code T} into a {@code R} type reader.
     * @param <R>    the type read by the returned reader
     * @return a reader that reads an object of type {@code T} and then reads an object of type {@code R}.
     */
    default <R> Reader<R> map(Function<? super T, Reader<? extends R>> mapper) {
        Objects.requireNonNull(mapper);
        return new Reader<>() {
            private Reader<? extends R> other;

            @Override
            public ProcessStatus process(ByteBuffer buffer) {
                Objects.requireNonNull(buffer);
                if (other == null) {
                    var status = Reader.this.process(buffer);
                    if (status != ProcessStatus.DONE) {
                        return status;
                    }
                    other = mapper.apply(Reader.this.get());
                    Reader.this.reset();
                }
                return other.process(buffer);
            }

            @Override
            public R get() {
                if (other == null) {
                    throw new IllegalStateException("Reader is not done.");
                }
                return other.get();
            }

            @Override
            public void reset() {
                if (other != null) {
                    other.reset();
                    other = null;
                }
                Reader.this.reset();
            }
        };
    }

    /**
     * Returns a composed {@code Reader} that performs, in sequence, the reading of an object of type {@code T} read by
     * {@code this}, which is then offered to the {@code action} consumer. It then behaves like a reader of type
     * {@code R} (reads an object of type {@code R} and returns it). <p>This method is designed primarily for storing
     * the object returned by this (through using the {@code action} consumer), to use it later in order to create a
     * more complex object by using the {@link Reader#andFinally(Function)} method, as demonstrated below:
     *
     * <blockquote><pre>
     * public record Point(int x, int y) {
     * }
     *
     * public static Reader&lt;Point&gt; pointReader(Reader&lt;Integer&gt; intReader) {
     *      Object context = new Object() {
     *          private int x;
     *      };
     *      return intReader.andThen(intReader, i -> context.x = i)
     *             .andFinally(y -> new Point(context.x, y));
     * }
     * </pre></blockquote>
     * <p>
     * It is important to note that the {@code andThen} methode is strictly equivalent to use the
     * {@link Reader#map(Function)} method with a lambda that do the side effect and then always returns the same reader.
     * <blockquote><pre>
     * Readers.intReader()
     *      .andThen(
     *          otherReader,
     *          i -> System.out.println(i)
     *      );
     * </pre></blockquote>
     * <p>
     * is equivalent to:
     * <blockquote><pre>
     * Readers.intReader()
     *         .map(i -> {
     *             System.out.println(i);
     *             return otherReader;
     *         });
     * </pre></blockquote>
     *
     * @param after  the reader that will read the new returned type
     * @param action the action to apply to the object returned by this after
     * @param <R>    the type of the object read by the second after
     * @return a composed {@code Reader} that performs in sequence the reading of an object of type {@code T} offered to
     * the {@code action} consumer and then reads an object of type {@code R}.
     */
    default <R> Reader<R> andThen(Reader<? extends R> after, Consumer<? super T> action) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(after);
        return map(t -> {
            action.accept(t);
            Reader.this.reset();
            return after;
        });
    }

    /**
     * Returns a reader that will apply a function to the {@code T} type object read by this reader in order to
     * transform it into an {@code R} type object before returning it.
     * <p>
     * Here is an example of usage:
     * <blockquote><pre>
     * Reader&lt;String&gt; intToStringReader = Readers.intReader()
     *      .andFinally(String::valueOf);
     * </pre></blockquote>
     * <p>
     * Which will create a reader that read an {@code int} and then convert it to a {@code String} before returning it.
     *
     * @param function the transformation to apply to the object read by this reader
     * @param <R>      the type of the object returned by this reader
     * @return a reader that will apply a function to the {@code T} type object read by this reader to transform it into
     * an {@code R} type object before returning it
     */
    default <R> Reader<R> andFinally(Function<? super T, ? extends R> function) {
        Objects.requireNonNull(function);
        return new Reader<>() {

            @Override
            public ProcessStatus process(ByteBuffer buffer) {
                Objects.requireNonNull(buffer);
                return Reader.this.process(buffer);
            }

            @Override
            public R get() {
                return function.apply(Reader.this.get());
            }

            @Override
            public void reset() {
                Reader.this.reset();
            }
        };
    }

    /**
     * Returns a reader that will, after reading a size {@code N} using the {@code sizeReader}, read {@code N} times
     * a {@code T} type object using this reader. The read objects will be accumulated in a container of type {@code A},
     * provided by the collector. This container will be transformed into an {@code R} type object, using the
     * {@link Collector#finisher()} of the collector before returning it.
     *
     * @param sizeReader the reader that reads the number {@code N} of times to read a {@code T} type object
     * @param collector  the collector that provides all the methods to stores the read objects
     * @param <A>        the type of the container used to store the objects read
     * @param <R>        the type of the object returned by the new reader
     * @return a reader that reads {@code N} times a {@code T} type object and accumulates them in a container before
     * returning the container transformed into a {@code R} type object
     */
    default <A, R> Reader<R> repeat(Reader<? extends Number> sizeReader, Collector<? super T, A, ? extends R> collector) {
        Objects.requireNonNull(sizeReader);
        Objects.requireNonNull(collector);
        return new Reader<>() {
            private int size = -1;
            private boolean error = false;
            private A container = collector.supplier().get();

            @Override
            public ProcessStatus process(ByteBuffer buffer) {
                Objects.requireNonNull(buffer);
                if (error || size == 0) {
                    throw new IllegalStateException("Reader is already done or in error state.");
                }

                if (size == -1) { // reads the size
                    var status = sizeReader.process(buffer);
                    if (status != ProcessStatus.DONE) {
                        error = status == ProcessStatus.ERROR;
                        return status;
                    }
                    size = sizeReader.get().intValue();
                    error = size < 0;
                    sizeReader.reset();
                }

                while (size > 0) { // reads the elements and accumulates them in the container
                    var status = Reader.this.process(buffer);
                    if (status != ProcessStatus.DONE) {
                        error = status == ProcessStatus.ERROR;
                        return status;
                    }
                    collector.accumulator().accept(container, Reader.this.get());
                    Reader.this.reset();
                    size--;
                }

                return ProcessStatus.DONE;
            }

            @Override
            public R get() {
                if (error || size != 0) {
                    throw new IllegalStateException("Reader is not done or in error state.");
                }
                return collector.finisher().apply(container);
            }

            @Override
            public void reset() {
                size = -1;
                Reader.this.reset();
                sizeReader.reset();
                container = collector.supplier().get();
                error = false;
            }
        };
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
