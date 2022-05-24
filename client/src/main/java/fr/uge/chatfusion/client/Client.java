package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.base.Sizes;
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
        key.attach(context);
        controller.launch();
    }

    public void shutdown() {
        System.out.println("Shutting down the client...");
        controller.shutdown();
    }

    private static boolean checkMessageSize(String message) {
        var isValid = Sizes.checkMessageSize(message);
        if (!isValid) {
            System.out.println("Message too long ! (max = " + Sizes.MAX_MESSAGE_SIZE + ")");
        }
        return isValid;
    }

    public void loginRefused() {
        System.out.println("Login failed. Closing the client...");
        shutdown();
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
            "Login successful. Welcome to "
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

    public void sendMessage(String input) {
        Objects.requireNonNull(input);
        if (!checkMessageSize(input)) {
            return;
        }
        var data = Frame.PublicMessage.buffer(serverName, login, input);
        controller.addCommand(() -> context.queueData(data));
    }

    public void sendDirectMessage(String dstSrv, String dstUser, String message) {
        Objects.requireNonNull(dstSrv);
        Objects.requireNonNull(dstUser);
        Objects.requireNonNull(message);
        if (serverName.equals(dstSrv) && login.equals(dstUser)) {
            System.out.println("You can't send a message to yourself !");
            return;
        }
        if (!checkMessageSize(message)) {
            return;
        }
        var data = Frame.DirectMessage.buffer(serverName, login, dstSrv, dstUser, message);
        controller.addCommand(() -> context.queueData(data));
    }

    public void receiveFileBlock(Frame.FileSending fileSending) {
        Objects.requireNonNull(fileSending);
        try {
            fileReceivingController.receiveFileBlock(fileSending);
        } catch (IOException e) {
            fileReceivingController.stopReceiving(fileSending);
            System.out.println("Error while receiving file");
        }
    }

    public void receivePublicMessage(Frame.PublicMessage publicMessage) {
        Objects.requireNonNull(publicMessage);
        var message = DateTimeUtils.printWithDateTime(publicMessage.format());
        System.out.println(message);
    }

    public void receiveDirectMessage(Frame.DirectMessage directMessage) {
        Objects.requireNonNull(directMessage);
        var message = DateTimeUtils.printWithDateTime(directMessage.format());
        System.out.println(message);
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
        controller.addCommand(() -> context.queueFile(serverName, login, dstSrv, dstUser, filePath));
    }
}
