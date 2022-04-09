package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.Sizes;

import java.io.IOException;

final class Application {
    private Application() {
        throw new AssertionError("No instances.");
    }

    private static void usage() {
        System.out.println("Usage : ChatFusionClient <host> <port> <username>");
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length != 3) {
            usage();
            return;
        }

        try {
            var host = args[0];
            var port = Integer.parseInt(args[1]);
            var login = args[2];
            if (!Sizes.checkUsernameSize(login)) {
                System.out.println("Username to long (max=" + Sizes.MAX_USERNAME_SIZE + ").");
            }
            var client = new Client(host, port, login);
            client.launch();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number" + args[1]);
        }
    }
}
