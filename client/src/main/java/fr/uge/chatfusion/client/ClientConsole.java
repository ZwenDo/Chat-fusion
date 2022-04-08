package fr.uge.chatfusion.client;

import java.util.Objects;
import java.util.Scanner;
import java.util.StringJoiner;

final class ClientConsole implements Runnable {
    private final Scanner scanner = new Scanner(System.in);
    private final Client client;

    ClientConsole(Client client) {
        Objects.requireNonNull(client);
        this.client = client;
    }

    @Override
    public void run() {
         while (scanner.hasNext()) {
            var input = scanner.nextLine();

            switch (input) {
                case "SHUTDOWN" -> client.shutdown();
                default -> processCommand(input);
            }
        }
    }

    private void processCommand(String input) {
        if (input.startsWith("/")) {
            System.out.println("File sending not implemented yet");
        } else if (input.startsWith("@")) {
            System.out.println("Direct message not implemented yet");
        } else {
            client.sendMessage(input);
        }
    }

}
