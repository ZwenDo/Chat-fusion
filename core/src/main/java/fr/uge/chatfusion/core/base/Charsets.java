package fr.uge.chatfusion.core.base;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Defines the charsets used to encode {@link String} in the Chatfusion protocol.
 */
public final class Charsets {
    private Charsets() {
        throw new AssertionError("No instances.");
    }

    /**
     * The default charset used in the Chatfusion protocol.
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
}
