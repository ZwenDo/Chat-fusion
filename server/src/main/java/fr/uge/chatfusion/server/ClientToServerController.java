package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.Sizes;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.selection.SelectionKeyController;
import fr.uge.chatfusion.server.visitor.IdentifiedRemoteInfo;
import fr.uge.chatfusion.server.visitor.UnknownRemoteInfo;
import fr.uge.chatfusion.server.visitor.Visitors;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

final class ClientToServerController {
    private static final Logger LOGGER = Logger.getLogger(ClientToServerController.class.getName());

    private final HashMap<String, SelectionKeyController> clients = new HashMap<>();
    private final String serverName;
    private final Server server;

    public ClientToServerController(String serverName, Server server) {
        Objects.requireNonNull(serverName);
        Objects.requireNonNull(server);
        this.serverName = serverName;
        this.server = server;
    }

    public void connectAnonymously(Frame.AnonymousLogin anonymousLogin, UnknownRemoteInfo infos) {
        Objects.requireNonNull(anonymousLogin);
        Objects.requireNonNull(infos);

        var username = anonymousLogin.username();
        if (!Sizes.checkUsernameSize(username)) {
            logMessageAndClose(
                Level.WARNING,
                "Invalid username size (" + username + ")",
                infos.address(),
                infos.connection()
            );
            return;
        }

        var controller = infos.controller();
        if (clients.putIfAbsent(username, controller) != null) {
            LOGGER.log(Level.WARNING, "Username already used (" + username + ")");
            controller.queueData(Frame.LoginRefused.buffer());
            controller.closeWhenAllSent();
            return;
        }

        // changing the visitor
        var userInfos = new IdentifiedRemoteInfo(username, infos.connection(), infos.address());
        controller.setVisitor(Visitors.loggedClientVisitor(server, userInfos));
        controller.setOnClose(() -> clients.remove(username));

        // answer to the client
        var data = Frame.LoginAccepted.buffer(serverName);
        controller.queueData(data);
    }

    public void sendPublicMessage(Frame.PublicMessage message, IdentifiedRemoteInfo remoteInfo) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(remoteInfo);
        clients.values()
            .forEach(client -> client.queueData(message.buffer()));
    }

    public String info() {
        var size = clients.size();
        if (size == 0) {
            return size + " connected client.\n";
        }
        var connectedList = clients.entrySet().stream()
            .map(e -> e.getKey() + " (" + e.getValue().remoteAddress() + ")")
            .collect(Collectors.joining("\n-"));
        return size + " connected client(s):\n-" + connectedList;
    }

    public void sendDirectMessage(Frame.DirectMessage message) {
        Objects.requireNonNull(message);
        sendData(message.recipientUsername(), message::buffer);
    }

    public void sendFile(Frame.FileSending fileSending) {
        Objects.requireNonNull(fileSending);
        if (!clients.containsKey(fileSending.recipientUsername())) {
            return;
        }
        sendData(fileSending.recipientUsername(), fileSending::buffer);
    }

    private void sendData(String recipientUsername, Supplier<ByteBuffer> data) {
        var recipient = clients.get(recipientUsername);
        if (recipient == null) {
            LOGGER.log(Level.INFO, "Receiver not found (" + recipientUsername + ")");
            return;
        }
        recipient.queueData(data.get());
    }

    private void logMessageAndClose(Level level, String message, InetSocketAddress address, Closeable closeable) {
        LOGGER.log(level, address + " : " + message);
        CloseableUtils.silentlyClose(closeable);
    }
}
