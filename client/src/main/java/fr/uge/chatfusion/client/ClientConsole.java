package fr.uge.chatfusion.client;

import java.util.Objects;
import java.util.Scanner;

final class ClientConsole implements Runnable {
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
        if (input.startsWith("/")) {
            processDirectMessage(input);
        } else if (input.startsWith("@")) {
            System.out.println("Direct message not implemented yet");
        } else {
            client.sendMessage(input);
        }
    }

    private void processDirectMessage(String input) {
        var args = input.split(" ", 2);
        var info = args[0].substring(1).split(":");
        var dstSrv = info[0];
        var dstUser = info[1];
        var message = args[1];
        client.sendDirectMessage(dstSrv, dstUser, message);
    }

}
