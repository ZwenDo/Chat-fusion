package fr.uge.chatfusion.core;

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
        return message.getBytes(StandardCharsets.UTF_8).length < MAX_MESSAGE_SIZE;
    }

    public static boolean checkUsernameSize(String username) {
        Objects.requireNonNull(username);
        return username.getBytes(StandardCharsets.UTF_8).length < MAX_USERNAME_SIZE;
    }

    public static boolean checkPasswordSize(String password) {
        Objects.requireNonNull(password);
        return password.getBytes(StandardCharsets.UTF_8).length < MAX_PASSWORD_SIZE;
    }

    public static boolean checkServerNameSize(String serverName) {
        Objects.requireNonNull(serverName);
        return serverName.getBytes(StandardCharsets.UTF_8).length < MAX_SERVER_NAME_SIZE;
    }
}
