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
                    processCommand(scanner, input);
                }
            }
        }
        client.shutdown();
    }

    private void processCommand(Scanner scanner, String input) {
        if (input.startsWith("/")) {
            System.out.println("File sending not implemented yet");
        } else if (input.startsWith("@")) {
            System.out.println("Direct message not implemented yet");
        } else {
            client.sendMessage(input);
        }
    }

}
