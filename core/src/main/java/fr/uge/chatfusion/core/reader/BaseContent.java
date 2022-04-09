package fr.uge.chatfusion.core.reader;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class BaseContent {
    private final ArrayDeque<Object> deque = new ArrayDeque<>();

    void add(Object o) {
        Objects.requireNonNull(o);
        deque.add(o);
    }

    int size() {
        return deque.size();
    }

    @SuppressWarnings("unchecked")
    public <T> T next() {
        Object next;
        try {
            next = deque.pop();
        } catch (NoSuchElementException e) {
            throw new ObjectConstructionException("Not enough elements.");
        }

        try {
            return (T) next;
        } catch (ClassCastException e) {
            throw new ObjectConstructionException("Wrong type, " + next.getClass().getSimpleName() + " expected.");
        }
    }
}
