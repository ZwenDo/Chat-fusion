package fr.uge.chatfusion.server.processor;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.Sizes;
import fr.uge.chatfusion.core.frame.AnonymousLogin;
import fr.uge.chatfusion.server.ServerInterface;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PreLogClientProcessor implements FrameProcessorClient {
    private static final Logger LOGGER = Logger.getLogger(PreLogClientProcessor.class.getName());
    private final ServerInterface server;
    private final SocketChannel channel;
    private final InetSocketAddress remoteAddress;
    private boolean isLogged;
    private String username;

    public PreLogClientProcessor(ServerInterface server, SocketChannel channel, InetSocketAddress remoteAddress) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(remoteAddress);
        this.server = server;
        this.channel = channel;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public FrameProcessorClient nextProcessor() {
        return isLogged ? new LoggedClientProcessor(server, channel, remoteAddress, username) : this;
    }

    @Override
    public void visit(AnonymousLogin anonymousLogin, Void context) {
        Objects.requireNonNull(anonymousLogin);

        if (isLogged) {
            logSevereAndClose("Already logged. Closing connection...");
            return;
        }

        if (!Sizes.checkUsernameSize(anonymousLogin.username())) {
            logSevereAndClose("Username too long. Closing connection...");
            return;
        }

        if (!server.tryToConnect(anonymousLogin.username())) {
            logSevereAndClose("Server refused the connection. Closing connection...");
            return;
        }

        isLogged = true;
        username = anonymousLogin.username();
    }

    private void logSevereAndClose(String message) {
        CloseableUtils.logForRemoteAndSilentlyClose(
            LOGGER,
            Level.SEVERE,
            message,
            remoteAddress,
            channel
        );
    }
}
