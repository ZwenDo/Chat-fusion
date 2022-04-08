package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.FrameBuilder;
import fr.uge.chatfusion.core.FrameOpcodes;
import fr.uge.chatfusion.core.Sizes;
import fr.uge.chatfusion.core.frame.PublicMessage;
import fr.uge.chatfusion.server.context.LoggedClientContext;
import fr.uge.chatfusion.server.context.DefaultContext;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

final class ClientToServerController {
    private static final Logger LOGGER = Logger.getLogger(ClientToServerController.class.getName());

    private final HashMap<String, LoggedClientContext> clients = new HashMap<>();
    private final String name;

    public ClientToServerController(String name) {
        Objects.requireNonNull(name);
        this.name = name;
    }

    public boolean tryToConnectAnonymously(String login, SelectionKey key) {
        Objects.requireNonNull(login);
        Objects.requireNonNull(key);

        var ctx = (DefaultContext) key.attachment();
        if (!Sizes.checkUsernameSize(login)) {
            logMessageAndClose(
                Level.WARNING,
                "Invalid username size (" + login + ")",
                ctx.remoteAddress(),
                ctx.socketChannel()
            );
            return false;
        }

        if (clients.containsKey(login)) {
            logMessageAndClose(
                Level.WARNING,
                "Username already connected (" + login + ")",
                ctx.remoteAddress(),
                ctx.socketChannel()
            );
            return false;
        }

        var client = ctx.asClientContext(login);
        key.attach(client);
        clients.put(login, client);
        var data = new FrameBuilder(FrameOpcodes.LOGIN_ACCEPTED)
            .addString(name)
            .build();
        client.queueData(data);

        return true;
    }

    public boolean sendPublicMessage(PublicMessage message) {
        Objects.requireNonNull(message);

        if (!Sizes.checkMessageSize(message.message())) {
            var ctx = clients.get(message.senderUsername());
            if (ctx == null) {
                LOGGER.log(
                    Level.WARNING,
                    "Message too long from: "
                        + message.originServer()
                        + "/" +message.senderUsername()
                );
            } else {
                logMessageAndClose(Level.WARNING, "Message too long", ctx.remoteAddress(), ctx.socketChannel());
            }
            return false;
        }

        var builder = new FrameBuilder(FrameOpcodes.PUBLIC_MESSAGE)
            .addString(message.originServer())
            .addString(message.senderUsername())
            .addString(message.message());
        clients.values().forEach(client -> client.queueData(builder.build()));

        return true;
    }

    public LoggedClientContext disconnectClient(String username) {
        Objects.requireNonNull(username);

        var ctx = clients.remove(username);
        if (ctx == null) {
            throw new IllegalArgumentException("Client not found: " + username);
        }
        CloseableUtils.silentlyClose(ctx.socketChannel());
        return ctx;
    }

    private void logMessageAndClose(Level level, String message, InetSocketAddress address, Closeable closeable) {
        LOGGER.log(level, address + " : " + message);
        CloseableUtils.silentlyClose(closeable);
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
}
