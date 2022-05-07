package fr.uge.chatfusion.server;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Scanner;

final class ServerConsole implements Runnable {
    private final Server server;

    public ServerConsole(Server server) {
        Objects.requireNonNull(server);
        this.server = server;
    }

    @Override
    public void run() {
        try (var scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                var input = scanner.nextLine();

                switch (input) {
                    case "INFO" -> server.info();
                    case "SHUTDOWN" -> server.shutdown();
                    case "SHUTDOWNNOW" -> {
                        server.shutdownNow();
                        return;
                    }
                    default -> {
                        if (input.startsWith("FUSION")) {
                            fusion(input);
                        } else {
                            System.out.println("Unknown command: " + input);
                        }
                    }
                }
            }
        }
    }

    private void fusion(String input) {
        var args = input.split(" ");
        if (args.length < 3) {
            System.out.println("Usage: FUSION <host> <port>");
            return;
        }
        var host = args[1];
        var strPort = args[2];
        try {
            var port = Integer.parseInt(strPort);
            var address = new InetSocketAddress(host, port);
            if (!server.initFusion(address)) {
                System.out.println("Server cannot fuse with itself.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid port: " + strPort);
        }
    }
}
