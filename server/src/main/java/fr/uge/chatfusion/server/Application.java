package fr.uge.chatfusion.server;

import java.io.IOException;

public final class Application {
    private Application() {
        throw new AssertionError("No instances.");
    }

    private static void usage() {
        System.out.println("Usage : ChatFusionServer serverName port");
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            usage();
            return;
        }

        try {
            var serverName = args[0];
            var port = Integer.parseInt(args[1]);
            var server = new Server(serverName, port);
            server.launch();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number" + args[1]);
        } catch (IOException e) {
            System.err.println("Error while creating the server");
        }
    }
}
