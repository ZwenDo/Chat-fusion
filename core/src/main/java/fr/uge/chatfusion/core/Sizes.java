package fr.uge.chatfusion.core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class Sizes {
    private Sizes() {
        throw new AssertionError("No instances.");
    }

    public static final int MAX_MESSAGE_SIZE = 1024;
    public static final int MAX_USERNAME_SIZE = 30;
    public static final int MAX_PASSWORD_SIZE = 30;
    public static final int MAX_SERVER_NAME_SIZE = 100;

    public static boolean checkMessageSize(String message) {
        Objects.requireNonNull(message);
        return checkSize(message, MAX_MESSAGE_SIZE);
    }

    public static boolean checkUsernameSize(String username) {
        Objects.requireNonNull(username);
        return checkSize(username, MAX_USERNAME_SIZE);
    }

    public static boolean checkPasswordSize(String password) {
        Objects.requireNonNull(password);
        return checkSize(password, MAX_PASSWORD_SIZE);
    }

    public static boolean checkServerNameSize(String serverName) {
        Objects.requireNonNull(serverName);
        return checkSize(serverName, MAX_SERVER_NAME_SIZE);
    }

    public static int stringSize(String string) {
        Objects.requireNonNull(string);
        return string.getBytes(Charsets.DEFAULT_CHARSET).length;
    }

    private static boolean checkSize(String string, int maxSize) {
        var size = stringSize(string);
        return size < maxSize && size > 0;
    }
}
