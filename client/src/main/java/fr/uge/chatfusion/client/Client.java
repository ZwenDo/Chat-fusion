package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.Sizes;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.selection.SelectionKeyController;
import fr.uge.chatfusion.core.selection.SelectionKeyControllerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Objects;

final class Client {
    private final String login;
    private final SocketChannelController controller;
    private String serverName;
    private final InetSocketAddress serverAddress;
    private SelectionKeyController context;

    public Client(String host, int port, String login) throws IOException {
        Objects.requireNonNull(host);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        Objects.requireNonNull(login);
        this.serverAddress = new InetSocketAddress(host, port);
        this.controller = new SocketChannelController(serverAddress);
        this.login = login;
    }

    public void launch() throws IOException {
        var key = controller.createSelectionKey();
        var skeyController = new SelectionKeyControllerImpl(key, serverAddress, false, false);
        skeyController.setVisitor(new UniqueVisitor(this));
        skeyController.setOnClose(() -> {
            System.out.println("An error occurred...");
            shutdown();
        });
        var data = Frame.AnonymousLogin.buffer(login);
        skeyController.queueData(data);
        key.attach(skeyController);
        context = skeyController;
        controller.launch();
    }

    public void shutdown() {
        System.out.println("Shutting down the client...");
        controller.shutdown();
    }

    public void loginAccepted(String serverName) {
        Objects.requireNonNull(serverName);
        if (!Sizes.checkServerNameSize(serverName)) {
            System.out.println(
                "Error, server name ("
                    + serverName
                    + ") is too long, max = "
                    + Sizes.MAX_SERVER_NAME_SIZE
            );
            shutdown();
            return;
        }
        System.out.println(
            "Login successful. Welcome on "
                + serverName
                + " "
                + login
                + "!"
                + " You can now start chatting."
        );
        this.serverName = serverName;
        var console = new Thread(new ClientConsole(this), "Client console");
        console.setDaemon(true);
        console.start();
    }

    public void loginRefused() {
        System.out.println("Login failed. Closing the client...");
        shutdown();
    }

    public void sendMessage(String input) {
        Objects.requireNonNull(input);
        if (!Sizes.checkMessageSize(input)) {
            System.out.println("Message too long !");
            return;
        }
        controller.addCommand(() -> {
            var data = Frame.PublicMessage.buffer(serverName, login, input);
            context.queueData(data);
        });
    }

    public void sendPrivateMessage(String targetServer, String targetUsername, String message) {

    }
}
