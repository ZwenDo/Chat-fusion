package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.FrameBuilder;
import fr.uge.chatfusion.core.FrameOpcodes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Objects;

final class Client {
    private final SocketChannel socketChannel = SocketChannel.open();
    private final InetSocketAddress serverAddress;
    private final String login;
    private final SocketChannelController controller;
    private final Selector selector = Selector.open();
    private UniqueContext context;
    private String serverName;

    public Client(String host, int port, String login) throws IOException {
        Objects.requireNonNull(host);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        Objects.requireNonNull(login);
        this.serverAddress = new InetSocketAddress(host, port);
        this.login = login;
        this.controller = new SocketChannelController(socketChannel, serverAddress, selector);
    }

    public void launch() throws IOException {
        controller.launch(this, (ctx) -> {
            context = ctx;
            var data = new FrameBuilder(FrameOpcodes.ANONYMOUS_LOGIN)
                .addString(login)
                .build();
            ctx.queueData(data);
        });
    }

    public void shutdown() {
        System.out.println("Shutting down the client...");
        CloseableUtils.silentlyClose(selector);
    }

    public void loginAccepted(String serverName) {
        Objects.requireNonNull(serverName);
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
        controller.addCommand(() -> {
            var data = new FrameBuilder(FrameOpcodes.PUBLIC_MESSAGE)
                .addString(serverName)
                .addString(login)
                .addString(input)
                .build();
            context.queueData(data);
        });
    }

    public void sendPrivateMessage(String targetServer, String targetUsername, String message) {

    }
}
