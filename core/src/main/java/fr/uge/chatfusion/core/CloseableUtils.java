package fr.uge.chatfusion.core;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public static void logForRemoteAndSilentlyClose(
        Logger logger,
        Level level,
        String message,
        InetSocketAddress remoteAddress,
        AutoCloseable closeable
    ) {
        Objects.requireNonNull(logger);
        Objects.requireNonNull(level);
        Objects.requireNonNull(message);
        Objects.requireNonNull(remoteAddress);
        Objects.requireNonNull(closeable);

        logger.log(level, remoteAddress + " : " + message);
        try {
            closeable.close();
        } catch (Exception e) {
            // ignore
        }
    }
}
