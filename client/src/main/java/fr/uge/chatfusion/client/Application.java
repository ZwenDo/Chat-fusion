package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.base.Sizes;

import java.io.IOException;
import java.nio.file.Path;

public final class Application {
    private Application() {
        throw new AssertionError("No instances.");
    }

    private static void usage() {
        System.out.println("Usage : ChatFusionClient <host> <port> <filePath> <username>");
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length != 4) {
            usage();
            return;
        }

        try {
            var host = args[0];
            var port = Integer.parseInt(args[1]);
            var filePath = Path.of(args[2]);
            if (!filePath.toFile().isDirectory()) {
                System.out.println("File path must be a directory.");
                return;
            }
            var login = args[3];
            if (!Sizes.checkUsernameSize(login)) {
                System.out.println("Username to long (max=" + Sizes.MAX_USERNAME_SIZE + ").");
                return;
            }
            var client = new Client(host, port, filePath, login);
            client.launch();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number" + args[1]);
        }
    }
}
