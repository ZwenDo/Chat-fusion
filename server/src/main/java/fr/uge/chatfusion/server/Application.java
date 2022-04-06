package fr.uge.chatfusion.server;

import java.io.IOException;

public final class Application {
    private Application() {
        throw new AssertionError("No instances.");
    }

    private static void usage() {
        System.out.println("Usage : ChatFusionServer port");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            usage();
            return;
        }

        try {
            var port = Integer.parseInt(args[0]);
            var server = new Server(port);
            server.launch();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number" + args[0]);
        } catch (IOException e) {
            System.err.println("Error while creating the server");
        }
    }
}
