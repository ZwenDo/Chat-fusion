package fr.uge.chatfusion.server;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Scanner;

final class ServerConsole implements Runnable {
    private final Scanner scanner = new Scanner(System.in);
    private final Server server;

    public ServerConsole(Server server) {
        Objects.requireNonNull(server);
        this.server = server;
    }

    @Override
    public void run() {
        while (scanner.hasNext()) {
            var input = scanner.next();

            switch (input) {
                case "INFO" -> server.info();
                case "SHUTDOWN" -> server.shutdown();
                case "SHUTDOWNNOW" -> {
                    server.shutdownNow();
                    return;
                }
                case "FUSION" -> fusion();
                default -> System.out.println("Unknown command: " + input);
            }
        }
    }

    private void fusion() {
        if (!scanner.hasNext()) {
            System.out.println("Usage: FUSION <host> <port>");
            return;
        }
        var host = scanner.next();
        if (!scanner.hasNext()) {
            System.out.println("Usage: FUSION <host> <port>");
            return;
        }
        var strPort = scanner.next();
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
