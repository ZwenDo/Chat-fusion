package fr.uge.chatfusion.core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class Charsets {
    private Charsets() {
        throw new AssertionError("No instances.");
    }

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
}
