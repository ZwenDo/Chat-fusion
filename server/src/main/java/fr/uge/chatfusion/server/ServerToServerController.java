package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.FrameBuilder;
import fr.uge.chatfusion.core.FrameOpcodes;
import fr.uge.chatfusion.core.Sizes;
import fr.uge.chatfusion.core.frame.*;
import fr.uge.chatfusion.server.context.DefaultContext;
import fr.uge.chatfusion.server.context.FusedServerContext;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

final class ServerToServerController {
    private static final Logger LOGGER = Logger.getLogger(ServerToServerController.class.getName());

    private final InetSocketAddress address;
    private final String serverName;
    private HashMap<String, FusedServerContext> fusedMembers = new HashMap<>();
    private HashSet<String> futureMembers = new HashSet<>();
    private FusedServerContext leader;
    private boolean isFusing;
    private final ArrayDeque<?> queuedFusions = new ArrayDeque<>();


    public ServerToServerController(InetSocketAddress address, String serverName) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(serverName);
        this.address = address;
        this.serverName = serverName;
    }

    public boolean tryFusion(Frame.FusionInit fusionInit, SelectionKey key, Consumer<SelectionKey> onLeadLost) {
        Objects.requireNonNull(fusionInit);
        Objects.requireNonNull(key);
        Objects.requireNonNull(onLeadLost);

        var ctx = (DefaultContext) key.attachment();
        // check if server is the leader
        if (leader != null) {
            var data = new FrameBuilder(FrameOpcodes.FUSION_INIT_FWD).build();
            ctx.queueData(data);
            ctx.closeWhenAllSent();
            return false;
        }

        // check if provided information are valid and compatible
        if (!checkFusion(fusionInit.serverName(), fusionInit.serverAddress(), fusionInit.members(), key)) {
            return false;
        }

        // accept the fusion
        var data = createFusionFrame(FrameOpcodes.FUSION_INIT_OK);
        ctx.queueData(data);

        // proceed to the fusion
        LOGGER.log(
            Level.INFO,
            "Fusion accepted with "
                + fusionInit.serverName()
                + " ("
                + fusionInit.serverAddress()
                + ")"
        );
        isFusing = true;
        fuse(fusionInit.serverName(), fusionInit.members(), fusionInit.serverAddress(), ctx, onLeadLost);
        return true;
    }

    public boolean mergeFusion(Frame.FusionMerge fusionMerge, SelectionKey key) {
        Objects.requireNonNull(fusionMerge);
        var ctx = (DefaultContext) key.attachment();

        var name = fusionMerge.name();
        if (!futureMembers.remove(name)) {
            logMessageAndClose(
                Level.SEVERE,
                "Wrong serverName. Closing connection...",
                ctx.remoteAddress(),
                ctx.socketChannel()
            );
            return false;
        }
        fusedMembers.put(name, ctx.asServerContext(name));
        return true;
    }

    public boolean checkFusion(
        String remoteName,
        InetSocketAddress remoteAddress,
        List<ServerInfo> members,
        SelectionKey key
    ) {
        Objects.requireNonNull(remoteName);
        Objects.requireNonNull(remoteAddress);
        Objects.requireNonNull(members);
        Objects.requireNonNull(key);

        // checking leader's name
        if (!Sizes.checkServerNameSize(remoteName) || this.serverName.equals(remoteName)) {
            logMessageAndClose(
                Level.SEVERE,
                "Invalid leader serverName (" + remoteName + "). Closing connection...",
                remoteAddress,
                key.channel()
            );
            return false;
        }

        // checking members' names
        var invalidServer = members.stream()
            .filter(s -> {
                var name = s.name();
                return this.serverName.equals(name) && !Sizes.checkServerNameSize(name) && !fusedMembers.containsKey(name);
            })
            .findAny();
        if (invalidServer.isPresent()) {
            logMessageAndClose(
                Level.SEVERE,
                "Invalid member (leader= "
                    + remoteName
                    + ") serverName ("
                    + invalidServer.get()
                    + "). Closing connection...",
                remoteAddress,
                key.channel()
            );
            return false;
        }

        return true;
    }

    public void fuse(
        String remoteName,
        List<ServerInfo> members,
        InetSocketAddress remoteAddress,
        DefaultContext ctx,
        Consumer<SelectionKey> onLeadLost
    ) {
        Objects.requireNonNull(remoteName);
        Objects.requireNonNull(members);
        Objects.requireNonNull(remoteAddress);
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(onLeadLost);
        if (!isFusing) {
            throw new IllegalStateException("Fusion is not in progress");
        }

        var other = ctx.asServerContext(remoteName);
        ctx.key().attach(other);

        var stillLeader = serverName.compareTo(remoteName) < 0;
        if (stillLeader) {
            LOGGER.log(Level.INFO, "Still leader");
            fusedMembers.put(remoteName, other);
            members.forEach(m -> futureMembers.add(m.name()));
            isFusing = false;
            return;
        }

        LOGGER.log(Level.INFO, remoteName + "(" + remoteAddress + ") is the new leader");
        leader = other;

        var builder = new FrameBuilder(FrameOpcodes.FUSION_CHANGE_LEADER)
            .addString(other.serverName())
            .addAddress(remoteAddress);
        fusedMembers.values().forEach(c -> {
            onLeadLost.accept(c.key());
            c.queueData(builder.build());
            c.closeWhenAllSent();
        });
        fusedMembers = new HashMap<>();
        isFusing = false;
    }

    public ByteBuffer createFusionFrame(byte opcode) {
        var infos = fusedMembers.entrySet()
            .stream()
            .map(k -> new ServerInfo(k.getKey(), k.getValue().remoteAddress()))
            .toList();
        return new FrameBuilder(opcode)
            .addString(serverName)
            .addAddress(address)
            .addInfoList(infos)
            .build();
    }

    private void logMessageAndClose(Level level, String message, InetSocketAddress address, Closeable closeable) {
        LOGGER.log(level, address + " : " + message);
        CloseableUtils.silentlyClose(closeable);
    }

    public void sendToAllExcept(FrameBuilder data, String originServer) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(originServer);
        fusedMembers.forEach((key, value) -> {
            if (originServer.equals(key)) return;
            value.queueData(data.build());
        });
    }

    public void initFusion(InetSocketAddress remote, Selector selector, Server server) throws IOException {
        Objects.requireNonNull(remote);
        Objects.requireNonNull(selector);
        Objects.requireNonNull(server);
        if (isFusing) {
            LOGGER.log(Level.WARNING, "Already fusing, ignoring..."); // TODO requeue the request
            return;
        }
        isFusing = true;

        if (leader != null) {
            LOGGER.log(Level.INFO, "Forwarding fusion request to leader");
            var data = new FrameBuilder(FrameOpcodes.FUSION_REQUEST)
                .addAddress(remote)
                .build();
            leader.queueData(data);
        } else {
            var sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(remote);
            var key = sc.register(selector, SelectionKey.OP_CONNECT);
            var ctx = new DefaultContext(server, key, remote, false);
            key.attach(ctx);

            var data = createFusionFrame(FrameOpcodes.FUSION_INIT);
            ctx.queueData(data);
        }
    }

    public FusedServerContext leader() {
        return leader;
    }

    public String info() {
        var size = fusedMembers.size();
        if (size == 0) {
            return size + " fused member.\n";
        }
        var connectedList = fusedMembers.entrySet().stream()
            .map(e -> e.getKey() + " (" + e.getValue().remoteAddress() + ")")
            .collect(Collectors.joining("\n-"));
        return size + " fused member(s):\n-" + connectedList;
    }

    public void changeLeader(
        Frame.FusionChangeLeader newLeader,
        SelectionKey key,
        Selector selector,
        Server server
    ) throws IOException {
        Objects.requireNonNull(newLeader);
        Objects.requireNonNull(key);
        Objects.requireNonNull(selector);
        Objects.requireNonNull(server);
        if (!Objects.equals(leader, key.attachment())) {
            logMessageAndClose(Level.SEVERE, "Change leader without being leader", address, key.channel());
            return;
        }

        LOGGER.log(Level.INFO, "Change leader to " + newLeader.leaderName() + "(" + newLeader.leaderAddress() + ")");
        var sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(newLeader.leaderAddress());
        var newLeaderKey = sc.register(selector, SelectionKey.OP_CONNECT);
        var ctx = new FusedServerContext(
            server,
            newLeader.leaderName(),
            newLeaderKey,
            newLeader.leaderAddress(),
            false
        );
        newLeaderKey.attach(ctx);
        leader = ctx;

        var data = new FrameBuilder(FrameOpcodes.FUSION_MERGE)
            .addString(serverName)
            .build();
        ctx.queueData(data);
    }

    public void rejectFusion(Frame.FusionInitKo fusionInitKo, SelectionKey key, InetSocketAddress address) {
        Objects.requireNonNull(fusionInitKo);
        Objects.requireNonNull(key);
        Objects.requireNonNull(address);
        LOGGER.log(Level.INFO, "Reject fusion request from " + address);
        isFusing = false;
    }
}
