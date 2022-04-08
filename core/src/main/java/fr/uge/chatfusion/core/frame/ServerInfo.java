package fr.uge.chatfusion.core.frame;

import java.net.InetSocketAddress;
import java.util.Objects;

public record ServerInfo(String name, InetSocketAddress address) {
    public ServerInfo {
        Objects.requireNonNull(name);
        Objects.requireNonNull(address);
    }
}
