package fr.uge.chatfusion.client;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

final class ClientConsole implements Runnable {
    private static final Pattern PATTERN_DIRECT_MESSAGE = Pattern.compile("@\\w+:\\w+ .+");
    private static final Pattern PATTERN_FILE_SENDING = Pattern.compile("/\\w+:\\w+ .+");

    private final Client client;

    ClientConsole(Client client) {
        Objects.requireNonNull(client);
        this.client = client;
    }

    @Override
    public void run() {
        try (var scanner = new Scanner(System.in)) {
            while (scanner.hasNext()) {
                var input = scanner.nextLine();

                if ("SHUTDOWN".equals(input)) {
                    client.shutdown();
                } else {
                    processCommand(input);
                }
            }
        }
        client.shutdown();
    }

    private void processCommand(String input) {
        if (input.startsWith("@")) {
            processDirectMessage(input);
        } else if (input.startsWith("/")) {
            processFileSending(input);
        } else {
            client.sendMessage(input);
        }
    }

    private void processDirectMessage(String input) {
        if (!PATTERN_DIRECT_MESSAGE.asMatchPredicate().test(input)) {
            System.out.println("Usage: @<server>:<recipient> <message>");
            return;
        }
        var args = input.split(" ", 2);
        var info = args[0].substring(1).split(":");
        var dstSrv = info[0];
        var dstUser = info[1];
        var message = args[1];
        client.sendDirectMessage(dstSrv, dstUser, message);
    }

    private void processFileSending(String input) {
        if (!PATTERN_FILE_SENDING.asMatchPredicate().test(input)) {
            System.out.println("Usage: /<server>:<recipient> <file>");
            return;
        }
        var args = input.split(" ", 2);
        var info = args[0].substring(1).split(":");
        var dstSrv = info[0];
        var dstUser = info[1];
        var filePath = Path.of(args[1]);
        client.sendFile(dstSrv, dstUser, filePath);
    }

}
