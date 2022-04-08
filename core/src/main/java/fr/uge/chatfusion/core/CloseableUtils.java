package fr.uge.chatfusion.core;

import java.util.Objects;

public final class CloseableUtils {
    private CloseableUtils() {
        throw new AssertionError("No instances.");
    }

    public static void silentlyClose(AutoCloseable closeable) {
        Objects.requireNonNull(closeable);
        try {
            closeable.close();
        } catch (Exception e) {
            // ignore
        }
    }

}
