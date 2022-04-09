package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.BufferUtils;
import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.Sizes;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.selection.SelectionKeyController;
import fr.uge.chatfusion.server.visitor.RemoteInfo;
import fr.uge.chatfusion.server.visitor.UnknownRemoteInfo;
import fr.uge.chatfusion.server.visitor.Visitors;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Objects;
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
        if (!checkLogin(anonymousLogin, infos)) {
            return;
        }

        var controller = infos.controller();
        clients.put(username, controller);

        // changing the visitor
        var userInfos = new RemoteInfo(username, infos.connection(), infos.address());
        controller.setVisitor(Visitors.loggedClientVisitor(server, userInfos));
        controller.setOnClose(() -> clients.remove(username));

        // answer to the client
        var data = Frame.LoginAccepted.buffer(serverName);
        controller.queueData(data);
    }

    public void sendPublicMessage(Frame.PublicMessage message, RemoteInfo remoteInfo) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(remoteInfo);

        var data = Frame.PublicMessage.buffer(
            message.originServer(),
            message.senderUsername(),
            message.message()
        );
        clients.values()
            .forEach(client -> client.queueData(BufferUtils.copy(data)));
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

    private boolean checkLogin(Frame.AnonymousLogin anonymousLogin, UnknownRemoteInfo infos) {
        var username = anonymousLogin.username();
        if (!Sizes.checkUsernameSize(username)) {
            logMessageAndClose(
                Level.WARNING,
                "Invalid username size (" + username + ")",
                infos.address(),
                infos.connection()
            );
            return false;
        }

        if (clients.containsKey(username)) {
            logMessageAndClose(
                Level.WARNING,
                "Username already connected (" + username + ")",
                infos.address(),
                infos.connection()
            );
            return false;
        }

        return true;
    }

    private void logMessageAndClose(Level level, String message, InetSocketAddress address, Closeable closeable) {
        LOGGER.log(level, address + " : " + message);
        CloseableUtils.silentlyClose(closeable);
    }
}
