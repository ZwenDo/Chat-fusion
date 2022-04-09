package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.FrameVisitor;

import java.util.Objects;

public final class Visitors {
    private Visitors() {
        throw new AssertionError("No instances.");
    }

    public static FrameVisitor loggedClientVisitor(ClientToServerInterface server, RemoteInfo infos) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(infos);
        return new LoggedClientVisitor(server, infos);
    }

    public static FrameVisitor fusedServerVisitor(ServerToServerInterface server, RemoteInfo infos) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(infos);
        return new FusedServerVisitor(server, infos);
    }

    public static FrameVisitor defaultVisitor(UnknownToServerInterface server, UnknownRemoteInfo infos) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(infos);
        return new DefaultVisitor(server, infos);
    }
}
