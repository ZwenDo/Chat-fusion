package fr.uge.chatfusion.core.frame;


import fr.uge.chatfusion.core.reader.Reader;
import fr.uge.chatfusion.core.reader.Readers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Defines the frames used to transfer data in the whole ChatFusion protocol.
 */
public sealed interface Frame {
    /**
     * Creates a frame reader.
     *
     * @return a new frame reader
     */
    static Reader<Frame> reader() {
        var byteReader = Readers.byteReader();
        var opcodeToReader = opcodeToReader(byteReader);
        return byteReader.compose()
            .map(b -> {
                byteReader.reset();
                return opcodeToReader.apply(b);
            })
            .toReader();
    }

    private static ByteBuffer fusionDataBuffer(
        String serverName,
        InetSocketAddress serverAddress,
        List<String> members,
        FrameOpcode opcode
    ) {
        return new FrameBuilder(opcode)
            .addString(serverName)
            .addAddress(serverAddress)
            .addStringList(members)
            .build();
    }

    //region Client frames

    private static Function<Byte, Reader<Frame>> opcodeToReader(Reader<Byte> byteReader) {
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

    /**
     * Accepts a frame visitor.
     *
     * @param visitor the visitor
     */
    void accept(FrameVisitor visitor);

    /**
     * Frame sent by the client to the server to ask for an anonymous connection.
     */
    record AnonymousLogin(String username) implements Frame {
        /**
         * Constructor.
         *
         * @param username the username of the user that wants to connect
         */
        public AnonymousLogin {
            Objects.requireNonNull(username);
        }
        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param username username of the user that wants to connect
         * @return the frame as a {@link ByteBuffer}
         */
        public static ByteBuffer buffer(String username) {
            Objects.requireNonNull(username);
            return new FrameBuilder(FrameOpcode.ANONYMOUS_LOGIN)
                .addString(username)
                .build();
        }

        /**
         * Creates a reader for the anonymous login frame.
         *
         * @param parts the different parts used to create the reader
         * @return a reader for the anonymous login frame
         */
        static Reader<Frame.AnonymousLogin> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return parts.string()
                .compose()
                .andFinally(AnonymousLogin::new)
                .toReader();
        }
    }
    //endregion

    //region Server frames

    /**
     * Frame sent by the server to the client to inform the client that the connection is accepted.
     */
    record LoginAccepted(String serverName) implements Frame {
        /**
         * Constructor.
         *
         * @param serverName the name of the server
         */
        public LoginAccepted {
            Objects.requireNonNull(serverName);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param serverName the name of the server
         * @return the frame as a {@link ByteBuffer}
         */
        public static ByteBuffer buffer(String serverName) {
            Objects.requireNonNull(serverName);
            return new FrameBuilder(FrameOpcode.LOGIN_ACCEPTED)
                .addString(serverName)
                .build();
        }

        /**
         * Creates a reader for the login accepted frame.
         *
         * @param parts the different parts used to create the reader
         * @return a reader for the login accepted frame
         */
        static Reader<Frame.LoginAccepted> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return parts.string()
                .compose()
                .andFinally(LoginAccepted::new)
                .toReader();
        }
    }

    /**
     * Frame sent by the server to the client to inform the client that the connection is rejected.
     */
    record LoginRefused() implements Frame {
        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @return the frame as a {@link ByteBuffer}
         */
        public static ByteBuffer buffer() {
            return new FrameBuilder(FrameOpcode.LOGIN_REFUSED).build();
        }

        /**
         * Creates a reader for the login refused frame.
         *
         * @param parts the different parts used to create the reader
         * @return a reader for the login refused frame
         */
        static Reader<Frame.LoginRefused> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.directReader(Frame.LoginRefused::new);
        }
    }

    /**
     * Frame sent from a server to another to initiate a fusion.
     */
    record FusionInit(String serverName, InetSocketAddress serverAddress, List<String> members) implements Frame {
        /**
         * Constructor.
         *
         * @param serverName the name of the server
         * @param serverAddress the address of the server
         * @param members the names of the members of the fusion
         */
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

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param serverName the name of the server
         * @param serverAddress the address of the server
         * @param members the names of the members of the server
         * @return the frame as a {@link ByteBuffer}
         */
        public static ByteBuffer buffer(String serverName, InetSocketAddress serverAddress, List<String> members) {
            Objects.requireNonNull(serverName);
            Objects.requireNonNull(serverAddress);
            Objects.requireNonNull(members);
            return Frame.fusionDataBuffer(serverName, serverAddress, members, FrameOpcode.FUSION_INIT);
        }

        /**
         * Creates a reader for the fusion init frame.
         *
         * @param parts the different parts used to create the reader
         * @return a reader for the fusion init frame
         */
        static Reader<Frame.FusionInit> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String serverName;
                InetSocketAddress serverAddress;
            };

            return parts.string()
                .compose()
                .andThen(parts.address(), s -> ctx.serverName = s)
                .andThen(parts.stringList(), a -> ctx.serverAddress = a)
                .andFinally(l -> new Frame.FusionInit(ctx.serverName, ctx.serverAddress, l))
                .toReader();
        }
    }

    /**
     * Frame sent from a server to another to accept a fusion.
     */
    record FusionInitOk(String serverName, InetSocketAddress serverAddress, List<String> members) implements Frame {
        /**
         * Constructor.
         *
         * @param serverName the name of the server
         * @param serverAddress the address of the server
         * @param members the names of the members of the fusion
         */
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

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param serverName the name of the server
         * @param serverAddress the address of the server
         * @param members the names of the members of the server
         * @return the frame as a {@link ByteBuffer}
         */
        public static ByteBuffer buffer(String serverName, InetSocketAddress serverAddress, List<String> members) {
            Objects.requireNonNull(serverName);
            Objects.requireNonNull(serverAddress);
            Objects.requireNonNull(members);
            return Frame.fusionDataBuffer(serverName, serverAddress, members, FrameOpcode.FUSION_INIT_OK);
        }

        /**
         * Creates a reader for the fusion init ok frame.
         *
         * @param parts the different parts used to create the reader
         * @return a reader for the fusion init ok frame
         */
        static Reader<Frame.FusionInitOk> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String serverName;
                InetSocketAddress serverAddress;
            };

            return parts.string()
                .compose()
                .andThen(parts.address(), s -> ctx.serverName = s)
                .andThen(parts.stringList(), a -> ctx.serverAddress = a)
                .andFinally(l -> new Frame.FusionInitOk(ctx.serverName, ctx.serverAddress, l))
                .toReader();
        }
    }

    /**
     * Frame sent from a server to another to refuse a fusion.
     */
    record FusionInitKo() implements Frame {
        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @return the frame as a {@link ByteBuffer}
         */
        public static ByteBuffer buffer() {
            return new FrameBuilder(FrameOpcode.FUSION_INIT_KO).build();
        }

        /**
         * Creates a reader for the fusion init ko frame.
         *
         * @param parts the different parts used to create the reader
         * @return a reader for the fusion init ko frame
         */
        static Reader<Frame.FusionInitKo> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return Readers.directReader(Frame.FusionInitKo::new);
        }
    }

    /**
     * Frame sent from a server to another in order to redirect to its leader.
     */
    record FusionInitFwd(InetSocketAddress leaderAddress) implements Frame {
        /**
         * Constructor.
         *
         * @param leaderAddress the address of the leader
         */
        public FusionInitFwd {
            Objects.requireNonNull(leaderAddress);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param leaderAddress the address of the leader
         * @return the frame as a {@link ByteBuffer}
         */
        public static ByteBuffer buffer(InetSocketAddress leaderAddress) {
            Objects.requireNonNull(leaderAddress);
            return new FrameBuilder(FrameOpcode.FUSION_INIT_FWD)
                .addAddress(leaderAddress)
                .build();
        }

        /**
         * Creates a reader for the fusion init fwd frame.
         *
         * @param parts the different parts used to create the reader
         * @return a reader for the fusion init fwd frame
         */
        static Reader<Frame.FusionInitFwd> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return parts.address()
                .compose()
                .andFinally(FusionInitFwd::new)
                .toReader();
        }
    }

    /**
     * Frame sent from a server to its leader to request for a fusion with an other server.
     */
    record FusionRequest(InetSocketAddress remote) implements Frame {
        /**
         * Constructor.
         *
         * @param remote the address of the remote server
         */
        public FusionRequest {
            Objects.requireNonNull(remote);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param remote the address of the remote server
         * @return the frame as a {@link ByteBuffer}
         */
        public static ByteBuffer buffer(InetSocketAddress remote) {
            Objects.requireNonNull(remote);
            return new FrameBuilder(FrameOpcode.FUSION_REQUEST)
                .addAddress(remote)
                .build();
        }

        /**
         * Creates a reader for the fusion request frame.
         *
         * @param parts the different parts used to create the reader
         * @return a reader for the fusion request frame
         */
        static Reader<Frame.FusionRequest> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return parts.address()
                .compose()
                .andFinally(FusionRequest::new)
                .toReader();
        }
    }

    /**
     * Frame sent from a server leader to its members to inform them that the leader of the group has changed.
     */
    record FusionChangeLeader(String leaderName, InetSocketAddress leaderAddress) implements Frame {
        /**
         * Constructor.
         *
         * @param leaderName the name of the new leader
         * @param leaderAddress the address of the new leader
         */
        public FusionChangeLeader {
            Objects.requireNonNull(leaderName);
            Objects.requireNonNull(leaderAddress);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param leaderName the name of the new leader
         * @param leaderAddress the address of the new leader
         * @return the frame as a {@link ByteBuffer}
         */
        public static ByteBuffer buffer(String leaderName, InetSocketAddress leaderAddress) {
            Objects.requireNonNull(leaderName);
            Objects.requireNonNull(leaderAddress);
            return new FrameBuilder(FrameOpcode.FUSION_CHANGE_LEADER)
                .addString(leaderName)
                .addAddress(leaderAddress)
                .build();
        }

        /**
         * Creates a reader for the fusion change leader frame.
         *
         * @param parts the different parts used to create the reader
         * @return a reader for the fusion change leader frame
         */
        static Reader<Frame.FusionChangeLeader> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String leaderName;
            };

            return parts.string()
                .compose()
                .andThen(parts.address(), s -> ctx.leaderName = s)
                .andFinally(a -> new FusionChangeLeader(ctx.leaderName, a))
                .toReader();
        }
    }
    //endregion

    //region Common frames

    /**
     * Frame sent from a server to its new leader to join the server group.
     */
    record FusionMerge(String name) implements Frame {
        /**
         * Constructor.
         *
         * @param name the name of the server that is joining the group
         */
        public FusionMerge {
            Objects.requireNonNull(name);
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param name the name of the server that will be merged
         * @return the frame as a {@link ByteBuffer}
         */
        public static ByteBuffer buffer(String name) {
            Objects.requireNonNull(name);
            return new FrameBuilder(FrameOpcode.FUSION_MERGE)
                .addString(name)
                .build();
        }

        /**
         * Creates a reader for the fusion merge frame.
         *
         * @param parts the different parts used to create the reader
         * @return a reader for the fusion merge frame
         */
        static Reader<Frame.FusionMerge> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            return parts.string()
                .compose()
                .andFinally(FusionMerge::new)
                .toReader();
        }
    }

    /**
     * Frame representing a public message. It can be sent from and to a client or a server.
     */
    record PublicMessage(String originServer, String senderUsername, String message) implements Frame {
        /**
         * Constructor.
         *
         * @param originServer the name of the server of the sender
         * @param senderUsername the username of the sender
         * @param message the message
         */
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

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param parts the different parts used to create the buffer
         * @return the frame as a {@link ByteBuffer}
         */
        static Reader<Frame.PublicMessage> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String originServer;
                String senderUsername;
            };

            return parts.string()
                .compose()
                .andThen(parts.string(), s -> ctx.originServer = s)
                .andThen(parts.string(), s -> ctx.senderUsername = s)
                .andFinally(s -> new PublicMessage(ctx.originServer, ctx.senderUsername, s))
                .toReader();
        }

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param originServer the origin server of the message
         * @param senderUsername the username of the sender
         * @param message the message
         * @return the frame as a {@link ByteBuffer}
         */
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

        /**
         * Creates a reader for the public message frame.
         *
         * @return a reader for the public message frame
         */
        public ByteBuffer buffer() {
            return buffer(originServer, senderUsername, message);
        }

        /**
         * Formats the frame message as a string.
         *
         * @return the frame message as a string
         */
        public String format() {
            return "[" + originServer + "] " + senderUsername + ": " + message;
        }
    }

    /**
     * Frame representing a private message. It can be sent from and to a client or a server.
     */
    record DirectMessage(
        String originServer,
        String senderUsername,
        String destinationServer,
        String recipientUsername,
        String message
    ) implements Frame {
        /**
         * Constructor.
         *
         * @param originServer the name of the server of the sender
         * @param senderUsername the username of the sender
         * @param destinationServer the name of the server of the recipient
         * @param recipientUsername the username of the recipient
         * @param message the message
         */
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

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param parts the different parts used to create the buffer
         * @return the frame as a {@link ByteBuffer}
         */
        static Reader<Frame.DirectMessage> reader(FrameReaderPart parts) {
            Objects.requireNonNull(parts);
            var ctx = new Object() {
                String originServer;
                String senderUsername;
                String destinationServer;
                String recipientUsername;
            };

            var str = parts.string();
            return str.compose()
                .andThen(str, s -> ctx.originServer = s)
                .andThen(str, s -> ctx.senderUsername = s)
                .andThen(str, s -> ctx.destinationServer = s)
                .andThen(str, s -> ctx.recipientUsername = s)
                .andFinally(s -> new DirectMessage(
                    ctx.originServer,
                    ctx.senderUsername,
                    ctx.destinationServer,
                    ctx.recipientUsername,
                    s
                ))
                .toReader();
        }

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @param originServer the origin server of the sender
         * @param senderUsername the username of the sender
         * @param destinationServer the destination server of the recipient
         * @param recipientUsername the username of the recipient
         * @param message the message
         * @return the frame as a {@link ByteBuffer}
         */
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

        /**
         * Creates a {@link ByteBuffer} in the frame format.
         *
         * @return the frame as a {@link ByteBuffer}
         */
        public ByteBuffer buffer() {
            return buffer(originServer, senderUsername, destinationServer, recipientUsername, message);
        }

        /**
         * Formats the frame message as a string.
         *
         * @return the frame message as a string
         */
        public String format() {
            return "[" + originServer + "] " + senderUsername + " whispers to you: " + message;
        }
    }
    //endregion

    /**
     * Frame representing a file transfer. It can be sent from and to a client or a server.
     */
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
        /**
         * Constructor.
         *
         * @param originServer the name of the server of the sender
         * @param senderUsername the username of the sender
         * @param destinationServer the name of the server of the recipient
         * @param recipientUsername the username of the recipient
         * @param fileId the file id
         * @param fileName the file name
         * @param blockCount the number of blocks
         * @param block the block
         */
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

        /**
         * Creates a {@link Reader} for the frame.
         *
         * @param originServer the origin server of the sender
         * @param senderUsername the username of the sender
         * @param destinationServer the destination server of the recipient
         * @param recipientUsername the username of the recipient
         * @param fileId the file id
         * @param fileName the file name
         * @param blockCount the number of total blocks
         * @param block the block
         * @return the frame as a {@link Reader}
         */
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

        /**
         * Creates a {@link Reader} for the frame.
         *
         * @param parts the parts of the frame
         * @return the frame as a {@link Reader}
         */
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
            return str.compose()
                .andThen(str, s -> ctx.originServer = s)
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
                ))
                .toReader();
        }

        @Override
        public void accept(FrameVisitor visitor) {
            Objects.requireNonNull(visitor);
            visitor.visit(this);
        }

        /**
         * Creates a {@link Reader} for the frame.
         *
         * @return the frame as a {@link Reader}
         */
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
