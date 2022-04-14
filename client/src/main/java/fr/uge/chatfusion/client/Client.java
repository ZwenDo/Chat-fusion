package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.Sizes;
import fr.uge.chatfusion.core.frame.Frame;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;

final class Client {
    private final String login;
    private final SocketChannelController controller;
    private final InetSocketAddress serverAddress;
    private final FileReceivingController fileReceivingController;
    private ClientKeyController context;
    private String serverName;

    public Client(String host, int port, Path filePath, String login) throws IOException {
        Objects.requireNonNull(host);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        Objects.requireNonNull(login);
        Objects.requireNonNull(filePath);
        this.serverAddress = new InetSocketAddress(host, port);
        this.controller = new SocketChannelController(serverAddress, this::shutdown);
        this.fileReceivingController = new FileReceivingController(filePath);
        this.login = login;
    }

    public void launch() throws IOException {
        var key = controller.createSelectionKey();
        context = new ClientKeyController(key, serverAddress);
        context.setVisitor(new UniqueVisitor(this));
        context.setOnClose(() -> {
            System.out.println("An error occurred...");
            shutdown();
        });
        var data = Frame.AnonymousLogin.buffer(login);
        context.queueData(data);
        context.processOut();
        key.attach(context);
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
        var data = Frame.PublicMessage.buffer(serverName, login, input);
        context.queueData(data);
        controller.addCommand(context::processOut);
    }

    public void sendDirectMessage(String dstSrv, String dstUser, String message) {
        Objects.requireNonNull(dstSrv);
        Objects.requireNonNull(dstUser);
        Objects.requireNonNull(message);
        if (!Sizes.checkMessageSize(message)) {
            System.out.println("Message too long !");
            return;
        }
        var data = Frame.DirectMessage.buffer(serverName, login, dstSrv, dstUser, message);
        context.queueData(data);
        controller.addCommand(context::processOut);
    }

    public void receiveFileBlock(Frame.FileSending fileSending) {
        try {
            fileReceivingController.receiveFileBlock(fileSending);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error while receiving file");
        }
    }

    public void sendFile(String dstSrv, String dstUser, Path filePath) {
        Objects.requireNonNull(dstSrv);
        Objects.requireNonNull(dstUser);
        Objects.requireNonNull(filePath);
        if (!Sizes.checkServerNameSize(dstSrv) || !Sizes.checkUsernameSize(dstUser)) {
            System.out.println("Error, server name or username too long !");
            return;
        }
        if (login.equals(dstUser) && serverName.equals(dstSrv)) {
            System.out.println("You can't send a file to yourself !");
            return;
        }
        context.queueFile(serverName, login, dstSrv, dstUser, filePath);
        controller.addCommand(context::processOut);
    }
}
