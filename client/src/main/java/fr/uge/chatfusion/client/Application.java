package fr.uge.chatfusion.client;

import java.io.IOException;

public final class Application {
    private Application() {
        throw new AssertionError("No instances.");
    }

    private static void usage() {
        System.out.println("Usage : ChatFusionClient host port login");
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length != 3) {
            usage();
            return;
        }

        try {
            var serverName = args[0];
            var port = Integer.parseInt(args[1]);
            var login = args[2];
            var client = new Client(serverName, port, login);
            client.launch();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number" + args[1]);
        }
    }
}
