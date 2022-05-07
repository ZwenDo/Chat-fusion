package fr.uge.chatfusion.client;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class DateTimeUtils {
    private static final DateTimeFormatter PATTERN = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");

    private DateTimeUtils() {
        throw new AssertionError("No instances.");
    }

    public static String printWithDateTime(String message) {
        Objects.requireNonNull(message);
        var now = LocalDateTime.now(ZoneId.systemDefault());
        var infos = now.format(PATTERN).split(" ", 2);
        return infos[0] + " at " + infos[1] + "\n  " + message;
    }
}
