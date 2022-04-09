package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.reader.base.Reader;

import java.util.Objects;
import java.util.function.Function;

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

    public static <T, R> Reader<T> sizedReader(
        Reader<R> reader,
        Function<Integer, ? extends T> factory,
        SizedReader.Function3<Integer, ? super R, ? super T, ? extends T> accumulator,
        Function<? super T, ? extends T> copier
    ) {
        Objects.requireNonNull(reader);
        Objects.requireNonNull(factory);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(copier);
        return new SizedReader<>(reader, factory, accumulator, copier);
    }

}
