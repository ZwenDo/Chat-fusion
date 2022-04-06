package fr.uge.chatfusion.server.processor;

import fr.uge.chatfusion.core.frame.PublicMessage;
import fr.uge.chatfusion.server.ServerInterface;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Objects;

public final class LoggedClientProcessor implements FrameProcessorClient {
    private final ServerInterface server;
    private final SocketChannel channel;
    private final InetSocketAddress remoteAddress;
    private final String username;

    public LoggedClientProcessor(ServerInterface server, SocketChannel channel, InetSocketAddress remoteAddress, String username) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(remoteAddress);
        Objects.requireNonNull(username);
        this.server = server;
        this.channel = channel;
        this.remoteAddress = remoteAddress;
        this.username = username;
    }

    @Override
    public FrameProcessorClient nextProcessor() {
        return this;
    }

    @Override
    public void visit(PublicMessage publicMessage, Void context) {
        Objects.requireNonNull(publicMessage);
        server.sendPublicMessage(remoteAddress, username, publicMessage.message());
    }
}
