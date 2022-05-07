package fr.uge.chatfusion.core.frame;


import fr.uge.chatfusion.core.reader.Reader;
import fr.uge.chatfusion.core.reader.Readers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public sealed interface Frame {
    void accept(FrameVisitor visitor);

    static Reader<Frame> reader() {
        var byteReader = Readers.byteReader();
        var opcodeToReader = opcodeToReader(byteReader);
        return byteReader.map(b -> {
            byteReader.reset();
            return opcodeToReader.apply(b);
        });
    }

    private static Function<Byte, Reader<? extends Frame>> opcodeToReader(Reader<Byte> byteReader) {
        var parts = FrameReaderPart.create(byteReader);
        @SuppressWarnings("unchecked")
        var readers = (Reader<Frame>[]) Arrays.stream(FrameOpcode.values())
            .map(op -> op.reader(parts))
            .toArray(Reader[]::new);

        return b -> {
            try {
                return readers[FrameOpcode.get(b).ordinal()];
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unknown opcode: " + b);
            }
        };
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
            return new FrameBuilder(FrameOpcode.ANONYMOUS_LOGIN)
                .addString(username)
                .build();
        }

        static Reader<Frame.AnonymousLogin> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return parts.string().andFinally(AnonymousLogin::new);
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
            return new FrameBuilder(FrameOpcode.LOGIN_ACCEPTED)
                .addString(serverName)
                .build();
        }

        static Reader<Frame.LoginAccepted> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return parts.string().andFinally(LoginAccepted::new);
        }
    }
    //endregion

    record LoginRefused() implements Frame {
        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer() {
            return new FrameBuilder(FrameOpcode.LOGIN_REFUSED).build();
        }

        static Reader<Frame.LoginRefused> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.directReader(Frame.LoginRefused::new);
        }
    }

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
            return new FrameBuilder(FrameOpcode.FUSION_INIT)
                .addString(serverName)
                .addAddress(serverAddress)
                .addStringList(members)
                .build();
        }

        static Reader<Frame.FusionInit> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String serverName;
                InetSocketAddress serverAddress;
            };

            return parts.string()
                .andThen(parts.address(), s -> ctx.serverName = s)
                .andThen(parts.stringList(), a -> ctx.serverAddress = a)
                .andFinally(l -> new Frame.FusionInit(ctx.serverName, ctx.serverAddress, l));
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
            return new FrameBuilder(FrameOpcode.FUSION_INIT_OK)
                .addString(serverName)
                .addAddress(serverAddress)
                .addStringList(members)
                .build();
        }

        static Reader<Frame.FusionInitOk> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String serverName;
                InetSocketAddress serverAddress;
            };

            return parts.string()
                .andThen(parts.address(), s -> ctx.serverName = s)
                .andThen(parts.stringList(), a -> ctx.serverAddress = a)
                .andFinally(l -> new Frame.FusionInitOk(ctx.serverName, ctx.serverAddress, l));
        }
    }

    record FusionInitKo() implements Frame {
        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public static ByteBuffer buffer() {
            return new FrameBuilder(FrameOpcode.FUSION_INIT_KO).build();
        }

        static Reader<Frame.FusionInitKo> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.directReader(Frame.FusionInitKo::new);
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
            return new FrameBuilder(FrameOpcode.FUSION_INIT_FWD)
                .addAddress(leaderAddress)
                .build();
        }

        static Reader<Frame.FusionInitFwd> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return parts.address().andFinally(FusionInitFwd::new);
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
            return new FrameBuilder(FrameOpcode.FUSION_REQUEST)
                .addAddress(remote)
                .build();
        }

        static Reader<Frame.FusionRequest> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return parts.address().andFinally(FusionRequest::new);
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
            return new FrameBuilder(FrameOpcode.FUSION_CHANGE_LEADER)
                .addString(leaderName)
                .addAddress(leaderAddress)
                .build();
        }

        static Reader<Frame.FusionChangeLeader> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String leaderName;
            };

            return parts.string()
                .andThen(parts.address(), s -> ctx.leaderName = s)
                .andFinally(a -> new FusionChangeLeader(ctx.leaderName, a));
        }
    }
    //endregion

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
            return new FrameBuilder(FrameOpcode.FUSION_MERGE)
                .addString(name)
                .build();
        }

        static Reader<Frame.FusionMerge> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return parts.string().andFinally(FusionMerge::new);
        }
    }

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

        static Reader<Frame.PublicMessage> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String originServer;
                String senderUsername;
            };

            return parts.string()
                .andThen(parts.string(), s -> ctx.originServer = s)
                .andThen(parts.string(), s -> ctx.senderUsername = s)
                .andFinally(s -> new PublicMessage(ctx.originServer, ctx.senderUsername, s));
        }

        public static ByteBuffer buffer(String originServer, String senderUsername, String message) {
            Objects.requireNonNull(originServer);
            Objects.requireNonNull(senderUsername);
            Objects.requireNonNull(message);
            return new FrameBuilder(FrameOpcode.PUBLIC_MESSAGE)
                .addString(originServer)
                .addString(senderUsername)
                .addString(message)
                .build();
        }

        public ByteBuffer buffer() {
            return buffer(originServer, senderUsername, message);
        }

        public String format() {
            return "[" + originServer + "] " + senderUsername + ": " + message;
        }
    }

    record DirectMessage(
        String originServer,
        String senderUsername,
        String destinationServer,
        String recipientUsername,
        String message
    ) implements Frame {
        public DirectMessage {
            Objects.requireNonNull(originServer);
            Objects.requireNonNull(senderUsername);
            Objects.requireNonNull(destinationServer);
            Objects.requireNonNull(recipientUsername);
            Objects.requireNonNull(message);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        static Reader<Frame.DirectMessage> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String originServer;
                String senderUsername;
                String destinationServer;
                String recipientUsername;
            };

            var str = parts.string();
            return str.andThen(str, s -> ctx.originServer = s)
                .andThen(str, s -> ctx.senderUsername = s)
                .andThen(str, s -> ctx.destinationServer = s)
                .andThen(str, s -> ctx.recipientUsername = s)
                .andFinally(s -> new DirectMessage(
                    ctx.originServer,
                    ctx.senderUsername,
                    ctx.destinationServer,
                    ctx.recipientUsername,
                    s
                ));
        }

        public static ByteBuffer buffer(
            String originServer,
            String senderUsername,
            String destinationServer,
            String recipientUsername,
            String message
        ) {
            Objects.requireNonNull(originServer);
            Objects.requireNonNull(senderUsername);
            Objects.requireNonNull(destinationServer);
            Objects.requireNonNull(recipientUsername);
            Objects.requireNonNull(message);
            return new FrameBuilder(FrameOpcode.DIRECT_MESSAGE)
                .addString(originServer)
                .addString(senderUsername)
                .addString(destinationServer)
                .addString(recipientUsername)
                .addString(message)
                .build();
        }

        public ByteBuffer buffer() {
            return buffer(originServer, senderUsername, destinationServer, recipientUsername, message);
        }

        public String format() {
            return "[" + originServer + "] " + senderUsername + " whispers to you: " + message;
        }
    }
    //endregion

    record FileSending(
        String originServer,
        String senderUsername,
        String destinationServer,
        String recipientUsername,
        long fileId,
        String fileName,
        int blockCount,
        ByteBuffer block
    ) implements Frame {
        public FileSending {
            Objects.requireNonNull(originServer);
            Objects.requireNonNull(senderUsername);
            Objects.requireNonNull(destinationServer);
            Objects.requireNonNull(recipientUsername);
            Objects.requireNonNull(fileName);
            if (blockCount <= 0) {
                throw new IllegalArgumentException("blockCount must be positive");
            }
            Objects.requireNonNull(block);
        }

        public static ByteBuffer buffer(
            String originServer,
            String senderUsername,
            String destinationServer,
            String recipientUsername,
            long fileId,
            String fileName,
            int blockCount,
            ByteBuffer block
        ) {
            Objects.requireNonNull(originServer);
            Objects.requireNonNull(senderUsername);
            Objects.requireNonNull(destinationServer);
            Objects.requireNonNull(recipientUsername);
            Objects.requireNonNull(fileName);
            if (blockCount <= 0) {
                throw new IllegalArgumentException("blockCount must be positive");
            }
            Objects.requireNonNull(block);
            return new FrameBuilder(FrameOpcode.FILE_SENDING)
                .addString(originServer)
                .addString(senderUsername)
                .addString(destinationServer)
                .addString(recipientUsername)
                .addLong(fileId)
                .addString(fileName)
                .addInt(blockCount)
                .addBuffer(block)
                .build();
        }

        static Reader<Frame.FileSending> buffer(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String originServer;
                String senderUsername;
                String destinationServer;
                String recipientUsername;
                long fileId;
                String fileName;
                int blockCount;
            };

            var str = parts.string();
            return str.andThen(str, s -> ctx.originServer = s)
                .andThen(str, s -> ctx.senderUsername = s)
                .andThen(str, s -> ctx.destinationServer = s)
                .andThen(parts.longInteger(), s -> ctx.recipientUsername = s)
                .andThen(str, l -> ctx.fileId = l)
                .andThen(parts.integer(), s -> ctx.fileName = s)
                .andThen(parts.byteBuffer(), i -> ctx.blockCount = i)
                .andFinally(b -> new Frame.FileSending(
                    ctx.originServer,
                    ctx.senderUsername,
                    ctx.destinationServer,
                    ctx.recipientUsername,
                    ctx.fileId,
                    ctx.fileName,
                    ctx.blockCount,
                    b
                ));
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        public ByteBuffer buffer() {
            return buffer(
                originServer,
                senderUsername,
                destinationServer,
                recipientUsername,
                fileId,
                fileName,
                blockCount,
                block
            );
        }
    }

}
