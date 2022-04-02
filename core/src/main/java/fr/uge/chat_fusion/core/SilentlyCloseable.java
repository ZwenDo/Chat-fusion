package fr.uge.chat_fusion.core;

import java.io.Closeable;

public interface SilentlyCloseable extends Closeable {
    default void silentlyClose() {
        try {
            close();
        } catch (Exception e) {
            // ignore
        }
    }
}
