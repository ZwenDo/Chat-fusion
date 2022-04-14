package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.reader.base.Reader;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public final class Readers {
    private Readers() {
        throw new AssertionError("No instances.");
    }


    public static <T> Reader<T> objectReader(
        Function<? super BaseContent, T> constructor,
        Reader<?>... readers
    ) {
        Objects.requireNonNull(constructor);
        Objects.requireNonNull(readers);
        return new ObjectReader<>(constructor, readers);
    }

    public static <T, A, R> Reader<R> sizedReader(
        Reader<? extends Number> sizeReader,
        Reader<? extends T> elementReader,
        Supplier<? extends A> factory,
        BiConsumer<? super A, ? super T> accumulator,
        Function<? super A, ? extends R> finisher
    ) {
        Objects.requireNonNull(sizeReader);
        Objects.requireNonNull(elementReader);
        Objects.requireNonNull(factory);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(finisher);
        return new SizedReader<>(sizeReader, elementReader, factory, accumulator, finisher);
    }

    public static <T, A, R> Reader<R> sizedReader(
        Reader<? extends Number> sizeReader,
        Reader<? extends T> elementReader,
        Collector<? super T, A, ? extends R> collector
    ) {
        Objects.requireNonNull(collector);
        return sizedReader(
            sizeReader,
            elementReader,
            collector.supplier(),
            collector.accumulator(),
            collector.finisher()
        );
    }

}
