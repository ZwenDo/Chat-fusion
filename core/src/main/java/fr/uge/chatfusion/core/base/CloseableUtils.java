package fr.uge.chatfusion.core.base;

import java.util.Objects;

/**
 * Defines utility methods for closing resources.
 */
public final class CloseableUtils {
    private CloseableUtils() {
        throw new AssertionError("No instances.");
    }

    /**
     * Close the given resource while ignoring any exceptions.
     *
     * @param closeable the resource to close
     */
    public static void silentlyClose(AutoCloseable closeable) {
        Objects.requireNonNull(closeable);
        try {
            closeable.close();
        } catch (Exception e) {
            // ignore
        }
    }

}
