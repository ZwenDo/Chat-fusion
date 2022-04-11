package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.Sizes;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.server.visitor.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

final class Server implements
    ClientToServerInterface, ServerToServerInterface, DefaultToServerInterface, PendingFusionToServerInterface {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private final Selector selector = Selector.open();
    private final ServerSocketChannelController controller;
    private final ClientToServerController serverClient;
    private final ServerToServerController serverServer;
    private final String serverName;
    private final InetSocketAddress address;

    public Server(String serverName, int port) throws IOException {
        Objects.requireNonNull(serverName);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        this.serverName = serverName;
        this.address = new InetSocketAddress(InetAddress.getLocalHost(), port);
        this.controller = new ServerSocketChannelController(this, address, selector);
        this.serverClient = new ClientToServerController(serverName, this);
        this.serverServer = new ServerToServerController(serverName, this, address);
    }

    public void launch() throws IOException {
        var console = new Thread(new ServerConsole(this), "Server console");
        console.setDaemon(true);
        console.start();

        LOGGER.log(Level.INFO, "Server started on port " + address.getPort());
        controller.launch();
    }

    @Override
    public void connectAnonymously(Frame.AnonymousLogin anonymousLogin, UnknownRemoteInfo infos) {
        serverClient.connectAnonymously(anonymousLogin, infos);
    }

    @Override
    public void tryFusion(Frame.FusionInit fusionInit, UnknownRemoteInfo infos) {
        serverServer.tryFusion(fusionInit, infos);
    }

    @Override
    public void fusionMerge(Frame.FusionMerge fusionMerge, UnknownRemoteInfo infos) {
        serverServer.fusionMerge(fusionMerge, infos);
    }

    @Override
    public void fusionAccepted(Frame.FusionInitOk fusionInitOk, UnknownRemoteInfo infos) {
        serverServer.acceptFusion(fusionInitOk, infos);
    }

    @Override
    public void fusionRejected(Frame.FusionInitKo fusionInitKo, UnknownRemoteInfo infos) {
        Objects.requireNonNull(fusionInitKo);
        Objects.requireNonNull(infos);
        serverServer.rejectFusion(fusionInitKo, infos);
    }

    @Override
    public void fusionForwarded(Frame.FusionInitFwd fusionInitFwd, UnknownRemoteInfo infos) {
        Objects.requireNonNull(fusionInitFwd);
        Objects.requireNonNull(infos);
        var address = fusionInitFwd.leaderAddress();
        if (address.getAddress().isLoopbackAddress() && address.getPort() == this.address.getPort()) {
            LOGGER.log(Level.WARNING, "Server tried to fusion with an other server of the group.");
            return;
        }
        logMessageAndClose(
            Level.INFO,
            "Fusion forwarded to the leader of the remote group (" + address + ")",
            infos.address(),
            infos.connection()
        );
        serverServer.forwardedFusion();
        initFusion(address);
    }

    @Override
    public void sendPublicMessage(Frame.PublicMessage message, IdentifiedRemoteInfo infos) {
        sendPublicMessage(message, infos, false);
    }

    @Override
    public void sendDirectMessage(Frame.DirectMessage message, IdentifiedRemoteInfo infos) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(infos);
        if (!Sizes.checkMessageSize(message.message())) {
            logMessageAndClose(
                Level.WARNING,
                "Message too long from: "
                    + message.originServer()
                    + "/" + message.senderUsername(),
                infos.address(),
                infos.connection()
            );
            return;
        }

        if (serverName.equals(message.destinationServer())) {
            serverClient.sendDirectMessage(message, infos);
        } else {
            serverServer.forwardDirectMessage(message, infos);
        }

    }

    @Override
    public void forwardPublicMessage(Frame.PublicMessage message, IdentifiedRemoteInfo infos) {
        sendPublicMessage(message, infos, true);
    }

    @Override
    public void fusionRequest(Frame.FusionRequest fusionRequest, IdentifiedRemoteInfo infos) {
        Objects.requireNonNull(fusionRequest);
        Objects.requireNonNull(infos);
        if (!serverServer.isLeader()) {
            logMessageAndClose(
                Level.SEVERE,
                "Received fusion request from " + infos.name() + " without being leader",
                infos.address(),
                infos.connection()
            );
            return;
        }
        var address = fusionRequest.remote();
        if (address.getAddress().isLoopbackAddress() && address.getPort() == this.address.getPort()) {
            LOGGER.log(
                Level.WARNING,
                infos.name() + "(" + infos.address() + ") tried to fusion with an other server of the group."
            );
            return;
        }
        LOGGER.log(Level.INFO, "Fusion request from " + infos.address());
        initFusion(fusionRequest.remote());
    }

    @Override
    public void changeLeader(Frame.FusionChangeLeader changeLeader, IdentifiedRemoteInfo infos) {
        Objects.requireNonNull(changeLeader);
        Objects.requireNonNull(infos);
        controller.addCommand(() -> {
            try {
                serverServer.changeLeader(changeLeader, infos, this::connectTo);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void sendPublicMessage(Frame.PublicMessage message, IdentifiedRemoteInfo infos, boolean isFwd) {
        if (!Sizes.checkMessageSize(message.message())) {
            logMessageAndClose(
                Level.WARNING,
                "Message too long from: "
                    + message.originServer()
                    + "/" + message.senderUsername(),
                infos.address(),
                infos.connection()
            );
            return;
        }

        if (!checkValidForward(message.originServer(), infos, isFwd)) {
            return;
        }

        if (!isFwd || serverServer.isLeader()) {
            if (!serverServer.tryForwardPublicMessage(message, infos)) {
                return;
            }
        }
        serverClient.sendPublicMessage(message, infos);
    }

    private boolean checkValidForward(String originServer, IdentifiedRemoteInfo infos, boolean isForwarded) {
        if (isForwarded && serverName.equals(originServer) ||
            !isForwarded && !serverName.equals(originServer)) {
            logMessageAndClose(
                Level.SEVERE,
                "Invalid origin server and forwarding state: " + originServer,
                infos.address(),
                infos.connection()
            );
            return false;
        }
        return true;
    }

    void shutdown() {
        LOGGER.log(Level.INFO, "Stop accepting new connections");
        controller.shutdown();
    }

    void shutdownNow() {
        LOGGER.log(Level.INFO, "Shutting down the server...");
        CloseableUtils.silentlyClose(selector);
    }

    boolean initFusion(InetSocketAddress remote) {
        Objects.requireNonNull(remote);
        if (address.getAddress().isLoopbackAddress() && address.getPort() == remote.getPort()) {
            return false;
        }
        controller.addCommand(() -> {
            try {
                serverServer.initFusion(remote, this::connectTo);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return true;
    }

    void info() {
        System.out.println(
            "Server Information:\nName = "
                + serverName + " (" + address + ")\n"
                + "\n"
                + serverServer.info()
                + "\n" +
                serverClient.info()
        );
    }

    private SelectionKey connectTo(SocketChannel channel) {
        try {
            return channel.register(selector, SelectionKey.OP_CONNECT);
        } catch (ClosedChannelException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void logMessageAndClose(Level level, String message, InetSocketAddress address, Closeable closeable) {
        LOGGER.log(level, address + " : " + message);
        CloseableUtils.silentlyClose(closeable);
    }

}
