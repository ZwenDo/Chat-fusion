package fr.uge.chatfusion.core.frame;

import fr.uge.chatfusion.core.reader.Readers;
import fr.uge.chatfusion.core.reader.base.Reader;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

public sealed interface Frame {
    void accept(FrameVisitor visitor);

    static Reader<Frame> reader() {
        return new FrameReader();
    }

    //region Client frames
    record AnonymousLogin(String username) implements Frame {
        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer(String username) {
            Objects.requireNonNull(username);
            return new FrameBuilder(FrameOpcodes.ANONYMOUS_LOGIN.value)
                .addString(username)
                .build();
        }

        static Reader<Frame.AnonymousLogin> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.objectReader(c -> new Frame.AnonymousLogin(c.next()), parts.string());
        }
    }

    record LoginAccepted(String serverName) implements Frame {
        public LoginAccepted {
            Objects.requireNonNull(serverName);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer(String serverName) {
            Objects.requireNonNull(serverName);
            return new FrameBuilder(FrameOpcodes.LOGIN_ACCEPTED.value)
                .addString(serverName)
                .build();
        }

        static Reader<Frame.LoginAccepted> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.objectReader(c -> new Frame.LoginAccepted(c.next()), parts.string());
        }
    }

    record LoginRefused() implements Frame {
        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer() {
            return new FrameBuilder(FrameOpcodes.LOGIN_REFUSED.value).build();
        }

        static Reader<Frame.LoginRefused> reader(FrameReaderPart parts) {
            return Readers.objectReader(c -> new Frame.LoginRefused());
        }
    }
    //endregion

    //region Server frames
    record FusionInit(String serverName, InetSocketAddress serverAddress, List<String> members) implements Frame {
        public FusionInit(String serverName, InetSocketAddress serverAddress, List<String> members) {
            Objects.requireNonNull(serverName);
            Objects.requireNonNull(serverAddress);
            Objects.requireNonNull(members);
            this.serverName = serverName;
            this.serverAddress = serverAddress;
            this.members = List.copyOf(members);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer(String serverName, InetSocketAddress serverAddress, List<String> members) {
            Objects.requireNonNull(serverName);
            Objects.requireNonNull(serverAddress);
            Objects.requireNonNull(members);
            return new FrameBuilder(FrameOpcodes.FUSION_INIT.value)
                .addString(serverName)
                .addAddress(serverAddress)
                .addStringList(members)
                .build();
        }

        static Reader<Frame.FusionInit> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.objectReader(
                c -> new Frame.FusionInit(c.next(), c.next(), c.next()),
                parts.string(),
                parts.address(),
                parts.stringList()
            );
        }
    }

    record FusionInitOk(String serverName, InetSocketAddress serverAddress, List<String> members) implements Frame {
        public FusionInitOk(String serverName, InetSocketAddress serverAddress, List<String> members) {
            Objects.requireNonNull(serverName);
            Objects.requireNonNull(serverAddress);
            Objects.requireNonNull(members);
            this.serverName = serverName;
            this.serverAddress = serverAddress;
            this.members = List.copyOf(members);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer(String serverName, InetSocketAddress serverAddress, List<String> members) {
            Objects.requireNonNull(serverName);
            Objects.requireNonNull(serverAddress);
            Objects.requireNonNull(members);
            return new FrameBuilder(FrameOpcodes.FUSION_INIT_OK.value)
                .addString(serverName)
                .addAddress(serverAddress)
                .addStringList(members)
                .build();
        }

        static Reader<Frame.FusionInitOk> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.objectReader(
                c -> new Frame.FusionInitOk(c.next(), c.next(), c.next()),
                parts.string(),
                parts.address(),
                parts.stringList()
            );
        }
    }

    record FusionInitKo() implements Frame {
        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer() {
            return new FrameBuilder(FrameOpcodes.FUSION_INIT_KO.value).build();
        }

        static Reader<Frame.FusionInitKo> reader(FrameReaderPart parts) {
            return Readers.objectReader(c -> new Frame.FusionInitKo());
        }

    }

    record FusionInitFwd(InetSocketAddress leaderAddress) implements Frame {
        public FusionInitFwd {
            Objects.requireNonNull(leaderAddress);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer(InetSocketAddress leaderAddress) {
            Objects.requireNonNull(leaderAddress);
            return new FrameBuilder(FrameOpcodes.FUSION_INIT_FWD.value)
                .addAddress(leaderAddress)
                .build();
        }

        static Reader<Frame.FusionInitFwd> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.objectReader(c -> new Frame.FusionInitFwd(c.next()), parts.string());
        }
    }

    record FusionRequest(InetSocketAddress remote) implements Frame {
        public FusionRequest {
            Objects.requireNonNull(remote);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer(InetSocketAddress remote) {
            Objects.requireNonNull(remote);
            return new FrameBuilder(FrameOpcodes.FUSION_REQUEST.value)
                .addAddress(remote)
                .build();
        }

        static Reader<Frame.FusionRequest> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.objectReader(c -> new Frame.FusionRequest(c.next()), parts.address());
        }
    }

    record FusionChangeLeader(String leaderName, InetSocketAddress leaderAddress) implements Frame {
        public FusionChangeLeader {
            Objects.requireNonNull(leaderName);
            Objects.requireNonNull(leaderAddress);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer(String leaderName, InetSocketAddress leaderAddress) {
            Objects.requireNonNull(leaderName);
            Objects.requireNonNull(leaderAddress);
            return new FrameBuilder(FrameOpcodes.FUSION_CHANGE_LEADER.value)
                .addString(leaderName)
                .addAddress(leaderAddress)
                .build();
        }

        static Reader<Frame.FusionChangeLeader> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.objectReader(
                c -> new Frame.FusionChangeLeader(c.next(), c.next()),
                parts.string(),
                parts.address()
            );
        }
    }

    record FusionMerge(String name) implements Frame {
        public FusionMerge {
            Objects.requireNonNull(name);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer(String name) {
            Objects.requireNonNull(name);
            return new FrameBuilder(FrameOpcodes.FUSION_MERGE.value)
                .addString(name)
                .build();
        }

        static Reader<Frame.FusionMerge> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.objectReader(c -> new Frame.FusionMerge(c.next()), parts.string());
        }
    }
    //endregion

    //region Common frames
    record PublicMessage(String originServer, String senderUsername, String message) implements Frame {
        public PublicMessage {
            Objects.requireNonNull(originServer);
            Objects.requireNonNull(senderUsername);
            Objects.requireNonNull(message);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer(String originServer, String senderUsername, String message) {
            Objects.requireNonNull(originServer);
            Objects.requireNonNull(senderUsername);
            Objects.requireNonNull(message);
            return new FrameBuilder(FrameOpcodes.PUBLIC_MESSAGE.value)
                .addString(originServer)
                .addString(senderUsername)
                .addString(message)
                .build();
        }

        static Reader<Frame.PublicMessage> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.objectReader(
                c -> new Frame.PublicMessage(c.next(), c.next(), c.next()),
                parts.string(),
                parts.string(),
                parts.string()
            );
        }

        public String format() {
            return "[" + originServer + "] " + senderUsername + ": " + message;
        }
    }
    //endregion
}
