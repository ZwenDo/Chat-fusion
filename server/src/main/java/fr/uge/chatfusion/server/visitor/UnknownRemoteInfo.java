package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.selection.SelectionKeyControllerImpl;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Objects;

public record UnknownRemoteInfo(
    SocketChannel connection,
    InetSocketAddress address,
    SelectionKeyControllerImpl controller
) {
    public UnknownRemoteInfo {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(address);
        Objects.requireNonNull(controller);
    }
}
