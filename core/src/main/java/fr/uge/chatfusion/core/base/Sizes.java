package fr.uge.chatfusion.core.base;

import java.util.Objects;

/**
 * Defines the size constants used in the Chatfusion protocol and provides methods to check if a size is valid.
 *
 * @apiNote The strings are supposed to be encoded in UTF-8 for the tests.
 */
public final class Sizes {
    private Sizes() {
        throw new AssertionError("No instances.");
    }

    /**
     * The maximum size of a message after it has been encoded.
     */
    public static final int MAX_MESSAGE_SIZE = 1024;

    /**
     * The maximum size of a username after it has been encoded.
     */
    public static final int MAX_USERNAME_SIZE = 30;

    /**
     * The maximum size of a password after it has been encoded.
     */
    public static final int MAX_SERVER_NAME_SIZE = 100;

    /**
     * The maximum size of a file transfer block.
     */
    public static final int MAX_FILE_BLOCK_SIZE = 3_000;

    /**
     * Checks if the size of a message is valid.
     *
     * @param message the message to check
     * @return true if the size is valid, false otherwise
     */
    public static boolean checkMessageSize(String message) {
        Objects.requireNonNull(message);
        return checkSize(message, MAX_MESSAGE_SIZE);
    }

    /**
     * Checks if the size of a username is valid.
     *
     * @param username the username to check
     * @return true if the size is valid, false otherwise
     */
    public static boolean checkUsernameSize(String username) {
        Objects.requireNonNull(username);
        return checkSize(username, MAX_USERNAME_SIZE);
    }

    /**
     * Checks if the size of a server name is valid.
     *
     * @param serverName the server name to check
     * @return true if the size is valid, false otherwise
     */
    public static boolean checkServerNameSize(String serverName) {
        Objects.requireNonNull(serverName);
        return checkSize(serverName, MAX_SERVER_NAME_SIZE);
    }

    private static boolean checkSize(String string, int maxSize) {
        var size = string.getBytes(Charsets.DEFAULT_CHARSET).length;
        return size < maxSize && size > 0;
    }
}
