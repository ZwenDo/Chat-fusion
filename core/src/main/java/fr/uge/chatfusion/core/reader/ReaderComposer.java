package fr.uge.chatfusion.core.reader;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * Represents an object that provides methods to compose {@link Reader}s in order to create more complex readers.
 *
 * @apiNote It is important to note that the inner reader is not copied on the composer creation, meaning that its state
 * can possibly be modified when the final reader (created using {@link ReaderComposer#toReader()} method) state
 * changes.
 *
 * @param <T> The type of objects read by the reader held by the composer
 */
public final class ReaderComposer<T> {
    private final Reader<T> inner;

    /**
     * Constructor.
     *
     * @param reader the inner reader of the composer
     */
    public ReaderComposer(Reader<T> reader) {
        Objects.requireNonNull(reader);
        this.inner = reader;
    }

    /**
     * Returns a composer that holds a reader which reads a {@code T} type object and maps it into and {@code R} type
     * reader. A simple example of usage could is a reader that reads a float or a double depending on the previous byte
     * read (for the example 0 is a float, and a different value is a double).
     * <blockquote><pre>
     * public static Reader&lt;Number&gt; pointReader() {
     *      return Readers.byteReader()
     *          .compose()
     *          .map(b -> b == 0
     *              ? Readers.doubleReader()
     *              : Readers.floatReader()
     *          )
     *          .toReader();
     * }
     * </pre></blockquote>
     *
     * @param mapper the function that transforms an object of type {@code T} into a {@code R} type reader.
     * @param <R>    the type read by the inner reader of the returned composer
     * @return a composer holding a reader that reads an object of type {@code T} and then reads an object of type
     * {@code R}.
     */
    public <R> ReaderComposer<R> map(Function<? super T, Reader<? extends R>> mapper) {
        Objects.requireNonNull(mapper);
        var result = new Reader<R>() {
            private Reader<? extends R> other;

            @Override
            public ProcessStatus process(ByteBuffer buffer) {
                Objects.requireNonNull(buffer);
                if (other == null) {
                    var status = inner.process(buffer);
                    if (status != ProcessStatus.DONE) {
                        return status;
                    }
                    other = mapper.apply(inner.get());
                    inner.reset();
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
                inner.reset();
            }
        };
        return new ReaderComposer<>(result);
    }

    /**
     * Returns a composer that holds a reader which performs, in sequence, the reading of an object of type {@code T}
     * read by {@code this}, which is then offered to the {@code action} consumer. It then behaves like a reader of type
     * {@code R} (reads an object of type {@code R} and returns it). <p>This method is designed primarily for storing
     * the object returned by this (through using the {@code action} consumer), to use it later in order to create a
     * more complex object by using the {@link ReaderComposer#andFinally(Function)} method, as demonstrated below:
     *
     * <blockquote><pre>
     * public record Point(int x, int y) {
     * }
     *
     * public static Reader&lt;Point&gt; pointReader(Reader&lt;Integer&gt; intReader) {
     *      Object context = new Object() {
     *          private int x;
     *      };
     *      return intReader.compose()
     *             .andThen(intReader, i -> context.x = i)
     *             .andFinally(y -> new Point(context.x, y))
     *             .toReader();
     * }
     * </pre></blockquote>
     * <p>
     * It is important to note that the {@code andThen} methode is strictly equivalent to use the
     * {@link ReaderComposer#map(Function)} method with a lambda that do the side effect and then always returns the
     * same reader.
     * <blockquote><pre>
     * Readers.intReader()
     *      .compose()
     *      .andThen(
     *          otherReader,
     *          i -> System.out.println(i)
     *      )
     *      .toReader();
     * </pre></blockquote>
     * <p>
     * is equivalent to:
     * <blockquote><pre>
     * Readers.intReader()
     *         .compose()
     *         .map(i -> {
     *             System.out.println(i);
     *             return otherReader;
     *         })
     *         .toReader();
     * </pre></blockquote>
     *
     * @param after  the reader that will read the new returned type
     * @param action the action to apply to the object returned by this after
     * @param <R>    the type of the object read by the after reader
     * @return a composer holding a reader which performs in sequence the reading of an object of type {@code T} offered
     * to the {@code action} consumer and then reads an object of type {@code R}.
     */
    public <R> ReaderComposer<R> andThen(Reader<? extends R> after, Consumer<? super T> action) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(after);
        return map(t -> {
            action.accept(t);
            inner.reset();
            return after;
        });
    }

    /**
     * Returns a composer that holds a reader which will apply a function to the {@code T} type object read by this
     * reader in order to transform it into an {@code R} type object before returning it.
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
     * @param <R>      the type of the object read by the reader contained by the returned composer
     * @return a composer that holds a reader which will apply a function to the {@code T} type object read by inner
     * reader to transform it into an {@code R} type object before returning it
     */
    public <R> ReaderComposer<R> andFinally(Function<? super T, ? extends R> function) {
        Objects.requireNonNull(function);
        var result = new Reader<R>() {

            @Override
            public ProcessStatus process(ByteBuffer buffer) {
                Objects.requireNonNull(buffer);
                return inner.process(buffer);
            }

            @Override
            public R get() {
                return function.apply(inner.get());
            }

            @Override
            public void reset() {
                inner.reset();
            }
        };
        return new ReaderComposer<>(result);
    }

    /**
     * Returns a composer that holds a reader which will, after reading a size {@code N} using the {@code sizeReader},
     * read {@code N} times a {@code T} type object using this reader. The read objects will be accumulated in a
     * container of type {@code A}, provided by the collector. This container will be transformed into an {@code R} type
     * object, using the {@link Collector#finisher()} of the collector before returning it.
     *
     * @param sizeReader the reader that reads the number {@code N} of times to read a {@code T} type object
     * @param collector  the collector that provides all the methods to stores the read objects
     * @param <A>        the type of the container used to store the objects read
     * @param <R>        the type of the object read by the reader of the returned composer
     * @return a composer holing a reader which reads {@code N} times a {@code T} type object and accumulates them in a
     * container before returning the container transformed into a {@code R} type object
     */
    public <A, R> ReaderComposer<R> repeat(Reader<? extends Number> sizeReader, Collector<? super T, A, ? extends R> collector) {
        Objects.requireNonNull(sizeReader);
        Objects.requireNonNull(collector);
        var result = new Reader<R>() {
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
                    var status = inner.process(buffer);
                    if (status != ProcessStatus.DONE) {
                        error = status == ProcessStatus.ERROR;
                        return status;
                    }
                    collector.accumulator().accept(container, inner.get());
                    inner.reset();
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
                inner.reset();
                sizeReader.reset();
                container = collector.supplier().get();
                error = false;
            }
        };
        return new ReaderComposer<>(result);
    }

    /**
     * Gets the inner reader of this composer.
     *
     * @return the inner reader of this composer
     */
    public Reader<T> toReader() {
        return inner;
    }
}
