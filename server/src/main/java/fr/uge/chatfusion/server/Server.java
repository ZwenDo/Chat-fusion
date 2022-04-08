package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.FrameBuilder;
import fr.uge.chatfusion.core.FrameOpcodes;
import fr.uge.chatfusion.core.frame.*;
import fr.uge.chatfusion.server.context.LoggedClientContext;
import fr.uge.chatfusion.server.context.Context;
import fr.uge.chatfusion.server.context.DefaultContext;
import fr.uge.chatfusion.server.context.FusedServerContext;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Server implements ClientToServerInterface, ServerToServerInterface {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private final ServerSocketChannelController controller;
    private final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    private final Selector selector = Selector.open();
    private final ClientToServerController serverClient;
    private final ServerToServerController serverServer;
    private final String serverName;

    private final InetSocketAddress address;
    private final HashMap<SelectionKey, String> keyToName = new HashMap<>();

    public Server(String serverName, int port) throws IOException {
        Objects.requireNonNull(serverName);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        this.serverName = serverName;
        this.serverSocketChannel.bind(new InetSocketAddress(port));
        this.controller = new ServerSocketChannelController(
            this,
            serverSocketChannel,
            selector,
            this::onException
        );
        this.address = new InetSocketAddress(InetAddress.getLocalHost(), port);

        this.serverClient = new ClientToServerController(serverName);
        this.serverServer = new ServerToServerController(address, serverName);
    }

    public void launch() throws IOException {
        var console = new Thread(new ServerConsole(this), "Server console");
        console.setDaemon(true);
        console.start();

        LOGGER.log(Level.INFO, "Server started on port " + address.getPort());
        controller.launch();
    }

    @Override
    public String name() {
        return serverName;
    }

    @Override
    public void tryToConnectAnonymously(String login, SelectionKey key) {
        if (serverClient.tryToConnectAnonymously(login, key)) {
            keyToName.put(key, login);
        }
    }

    @Override
    public void sendPublicMessage(Frame.PublicMessage message) {
        Objects.requireNonNull(message);
        sendPublicMessage(message, false);
    }

    private void sendPublicMessage(Frame.PublicMessage message, boolean isFwd) {
        if (!serverClient.sendPublicMessage(message)) {
            return;
        }
        var data = new FrameBuilder(FrameOpcodes.PUBLIC_MESSAGE)
            .addString(message.originServer())
            .addString(message.senderUsername())
            .addString(message.message());
        if (serverServer.leader() == null) {
            serverServer.sendToAllExcept(data, message.originServer());
        } else if (!isFwd) {
            serverServer.leader().queueData(data.build());
        }
    }

    @Override
    public void forwardPublicMessage(Frame.PublicMessage message) {
        Objects.requireNonNull(message);
        sendPublicMessage(message, true);
    }

    @Override
    public void fusionRequest(Frame.FusionRequest fusionRequest, SelectionKey key, InetSocketAddress address) {
        if (serverServer.leader() != null) {
            logMessageAndClose(Level.SEVERE, "Fusion request without being leader", address, key.channel());
            return;
        }

        LOGGER.log(Level.INFO, "Fusion request from " + address);
        if (!initFusion(fusionRequest.remote())) {
            logMessageAndClose(Level.SEVERE, "Server cannot fuse with itself.", address, key.channel());
        }
    }

    @Override
    public void changeLeader(Frame.FusionChangeLeader changeLeader, SelectionKey key, InetSocketAddress address) {
        Objects.requireNonNull(changeLeader);
        Objects.requireNonNull(key);
        Objects.requireNonNull(address);
        try {
            serverServer.changeLeader(changeLeader, key, selector, this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void rejectFusion(Frame.FusionInitKo fusionInitKo, SelectionKey key, InetSocketAddress address) {
        Objects.requireNonNull(fusionInitKo);
        Objects.requireNonNull(key);
        Objects.requireNonNull(address);
        serverServer.rejectFusion(fusionInitKo, key, address);
    }

    @Override
    public void disconnectClient(String username) {
        var client = serverClient.disconnectClient(username);
        keyToName.remove(client.key());
    }

    @Override
    public void tryFusion(Frame.FusionInit fusionInit, SelectionKey key) {
        serverServer.tryFusion(fusionInit, key, keyToName::remove);
    }

    @Override
    public void mergeFusion(Frame.FusionMerge fusionMerge, SelectionKey key) {
        Objects.requireNonNull(fusionMerge);
        Objects.requireNonNull(key);
        if (serverServer.mergeFusion(fusionMerge, key)) {
            keyToName.put(key, fusionMerge.name());
        }
    }

    @Override
    public InetSocketAddress leaderAddress() {
        var leader = serverServer.leader();
        return leader != null ? leader.remoteAddress() : address;
    }

    @Override
    public void acceptFusion(Frame.FusionInitOk fusionInitOk, SelectionKey key) {
        Objects.requireNonNull(fusionInitOk);
        Objects.requireNonNull(key);
        if (!serverServer.checkFusion(
            fusionInitOk.serverName(),
            fusionInitOk.serverAddress(),
            fusionInitOk.members(),
            key
        )) {
            return;
        }

        LOGGER.log(Level.INFO,
            "Fusion accepted by distant server "
                + fusionInitOk.serverName()
                + "("
                + fusionInitOk.serverAddress()
                + ")"
        );
        var ctx = (DefaultContext) key.attachment();
        serverServer.fuse(
            fusionInitOk.serverName(),
            fusionInitOk.members(),
            fusionInitOk.serverAddress(),
            ctx,
            keyToName::remove
        );

    }

    void shutdown() {
        LOGGER.log(Level.INFO, "Stop accepting new connections");
        CloseableUtils.silentlyClose(serverSocketChannel);
    }

    void shutdownNow() {
        LOGGER.log(Level.INFO, "Shutting down the server...");
        CloseableUtils.silentlyClose(selector);
    }

    void info() {
        var leader = serverServer.leader();
        System.out.println(
            "Server Information:\nName = "
                + serverName
                + "\nLeader = "
                + (leader != null ? leader.serverName() + " (" + leader.remoteAddress() + ")\n\n" : "self\n\n")
                + serverServer.info()
                + "\n" +
                serverClient.info()
        );
    }

    boolean initFusion(InetSocketAddress remote) {
        Objects.requireNonNull(remote);
        if (address.equals(remote)) {
            return false;
        }
        controller.addCommand(() -> {
            try {
                serverServer.initFusion(remote, selector, this);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return true;
    }

    private void logMessageAndClose(Level level, String message, InetSocketAddress address, Closeable closeable) {
        LOGGER.log(level, address + " : " + message);
        CloseableUtils.silentlyClose(closeable);
    }

    private void onException(IOException e, SelectionKey key) {
        Objects.requireNonNull(e);
        Objects.requireNonNull(key);
        LOGGER.log(Level.INFO, "Connection closed due to IOException", e);

        var name = keyToName.remove(key);

        var ctx = (Context) key.attachment();
        if (ctx instanceof DefaultContext) {
            CloseableUtils.silentlyClose(key.channel());
        } else if (ctx instanceof LoggedClientContext) {
            disconnectClient(name);
        } else if (ctx instanceof FusedServerContext) {
            CloseableUtils.silentlyClose(key.channel());
        } else {
            throw new AssertionError("Impossible state.");
        }
    }
}
