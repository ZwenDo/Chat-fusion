package fr.uge.chatfusion.server.visitor;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Objects;

public record IdentifiedRemoteInfo(String name, SocketChannel connection, InetSocketAddress address) {
    public IdentifiedRemoteInfo {
        Objects.requireNonNull(name);
        Objects.requireNonNull(connection);
        Objects.requireNonNull(address);
    }
}
