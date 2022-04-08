package fr.uge.chatfusion.core.frame;

import fr.uge.chatfusion.core.reader.base.Reader;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public sealed interface Frame {
    <C> void accept(FrameVisitor<C> frameVisitor, C context);


    class Util {
        private Util() {
            throw new AssertionError("No insances.");
        }

        private static final Map<Object, Object> ntr = new HashMap<>();

        public static Object ntr(Object o) {
            return ntr.get(o);
        }
    }



    record AnonymousLogin(String username) implements Frame {
        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            Objects.requireNonNull(frameVisitor);
            frameVisitor.visit(this, context);
        }
    }

    record FusionChangeLeader(String leaderName, InetSocketAddress leaderAddress) implements Frame {
        public FusionChangeLeader {
            Objects.requireNonNull(leaderName);
            Objects.requireNonNull(leaderAddress);
        }

        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            Objects.requireNonNull(frameVisitor);
            frameVisitor.visit(this, context);
        }
    }

    record FusionInit(String serverName, InetSocketAddress serverAddress, List<ServerInfo> members) implements Frame {
        public FusionInit(String serverName, InetSocketAddress serverAddress, List<ServerInfo> members) {
            Objects.requireNonNull(serverName);
            Objects.requireNonNull(serverAddress);
            Objects.requireNonNull(members);
            this.serverName = serverName;
            this.serverAddress = serverAddress;
            this.members = List.copyOf(members);
        }

        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            Objects.requireNonNull(frameVisitor);
            frameVisitor.visit(this, context);
        }
    }

    record FusionInitKo() implements Frame {
        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            Objects.requireNonNull(frameVisitor);
            frameVisitor.visit(this, context);
        }
    }

    record LoginRefused() implements Frame {
        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            frameVisitor.visit(this, context);
        }

        void foo() {

        }
    }

    record FusionMerge(String name) implements Frame {
        public FusionMerge {
            Objects.requireNonNull(name);
        }

        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            Objects.requireNonNull(frameVisitor);
            frameVisitor.visit(this, context);
        }
    }

    record LoginAccepted(String serverName) implements Frame {
        public LoginAccepted {
            Objects.requireNonNull(serverName);
        }

        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            Objects.requireNonNull(frameVisitor);
            frameVisitor.visit(this, context);
        }
    }

    record FusionInitOk(String serverName, InetSocketAddress serverAddress, List<ServerInfo> members) implements Frame {
        public FusionInitOk(String serverName, InetSocketAddress serverAddress, List<ServerInfo> members) {
            Objects.requireNonNull(serverName);
            Objects.requireNonNull(serverAddress);
            Objects.requireNonNull(members);
            this.serverName = serverName;
            this.serverAddress = serverAddress;
            this.members = List.copyOf(members);
        }

        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            Objects.requireNonNull(frameVisitor);
            frameVisitor.visit(this, context);
        }
    }

    record FusionInitFwd(InetSocketAddress leaderAddress) implements Frame {
        public FusionInitFwd {
            Objects.requireNonNull(leaderAddress);
        }

        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            Objects.requireNonNull(frameVisitor);
            frameVisitor.visit(this, context);
        }
    }

    record FusionRequest(InetSocketAddress remote) implements Frame {
        public FusionRequest {
            Objects.requireNonNull(remote);
        }

        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            Objects.requireNonNull(frameVisitor);
            frameVisitor.visit(this, context);
        }
    }

    record PublicMessage(String originServer, String senderUsername, String message) implements Frame {
        public PublicMessage {
            Objects.requireNonNull(originServer);
            Objects.requireNonNull(senderUsername);
            Objects.requireNonNull(message);
        }

        @Override
        public <C> void accept(FrameVisitor<C> frameVisitor, C context) {
            Objects.requireNonNull(frameVisitor);
            frameVisitor.visit(this, context);
        }

        public String format() {
            return "[" + originServer + "] " + senderUsername + ": " + message;
        }
    }
}
