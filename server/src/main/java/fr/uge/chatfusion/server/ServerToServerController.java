package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.base.BufferUtils;
import fr.uge.chatfusion.core.base.CloseableUtils;
import fr.uge.chatfusion.core.base.Sizes;
import fr.uge.chatfusion.core.frame.Frame;
import fr.uge.chatfusion.core.selection.SelectionKeyController;
import fr.uge.chatfusion.core.selection.SelectionKeyControllerImpl;
import fr.uge.chatfusion.server.visitor.IdentifiedRemoteInfo;
import fr.uge.chatfusion.server.visitor.UnknownRemoteInfo;
import fr.uge.chatfusion.server.visitor.Visitors;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

final class ServerToServerController {
    private static final Logger LOGGER = Logger.getLogger(ServerToServerController.class.getName());

    private HashMap<String, SelectionKeyController> members = new HashMap<>();
    private final String serverName;
    private final Server server;
    private final InetSocketAddress address;
    private final HashSet<String> futureMembers = new HashSet<>();
    private ServerLeader leader;
    private boolean isFusing;


    public ServerToServerController(String serverName, Server server, InetSocketAddress address) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(server);
        Objects.requireNonNull(serverName);
        this.address = address;
        this.server = server;
        this.serverName = serverName;
    }

    public void tryFusion(Frame.FusionInit fusionInit, UnknownRemoteInfo infos) {
        Objects.requireNonNull(fusionInit);
        Objects.requireNonNull(infos);
        if (isFusing) {
            logMessageAndClose(
                Level.WARNING,
                "Fusion already in progress",
                infos.address(),
                infos.connection()
            );
            return;
        }

        var ctx = infos.controller();
        // check if server is the leader
        if (leader != null) {
            LOGGER.log(Level.INFO, "Server is not the leader. Sending leader address...");
            var data = Frame.FusionInitFwd.buffer(leader.infos().address());
            ctx.queueData(data);
            ctx.closeWhenAllSent();
            return;
        }

        // check if provided information are valid and compatible
        if (!checkFusion(fusionInit.serverName(), fusionInit.members(), infos)) {
            return;
        }

        // accept the fusion
        var data = Frame.FusionInitOk.buffer(serverName, address, new ArrayList<>(members.keySet()));
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
        fuse(fusionInit.serverName(), fusionInit.members(), fusionInit.serverAddress(), infos);
    }

    public void acceptFusion(Frame.FusionInitOk fusionInitOk, UnknownRemoteInfo infos) {
        Objects.requireNonNull(fusionInitOk);
        Objects.requireNonNull(infos);
        if (!checkFusion(fusionInitOk.serverName(), fusionInitOk.members(), infos)) {
            return;
        }

        LOGGER.log(Level.INFO,
            "Fusion accepted by distant server "
                + fusionInitOk.serverName()
                + "("
                + fusionInitOk.serverAddress()
                + ")"
        );
        fuse(fusionInitOk.serverName(), fusionInitOk.members(), fusionInitOk.serverAddress(), infos);
    }

    public void fusionMerge(Frame.FusionMerge fusionMerge, UnknownRemoteInfo infos) {
        Objects.requireNonNull(fusionMerge);
        Objects.requireNonNull(infos);

        var name = fusionMerge.name();
        if (!futureMembers.remove(name)) {
            logMessageAndClose(
                Level.SEVERE,
                "Wrong serverName. Closing connection...",
                infos.address(),
                infos.connection()
            );
        }
        var ctx = infos.controller();
        var serverInfos = new IdentifiedRemoteInfo(fusionMerge.name(), infos.connection(), infos.address());
        ctx.setVisitor(Visitors.fusedServerVisitor(server, serverInfos));
        members.put(name, infos.controller());
        if (futureMembers.isEmpty()) {
            LOGGER.log(Level.INFO, "Fusion complete");
            isFusing = false;
        }
    }

    private boolean checkFusion(
        String remoteName,
        List<String> remoteMembers,
        UnknownRemoteInfo infos
    ) {
        // checking leader's name
        if (!Sizes.checkServerNameSize(remoteName) || serverName.equals(remoteName)) {
            logMessageAndClose(
                Level.SEVERE,
                "Invalid leader serverName (" + remoteName + "). Closing connection...",
                infos.address(),
                infos.connection()
            );
            return false;
        }

        // checking members' names
        var invalidServer = remoteMembers.stream()
            .filter(s -> !(serverName.equals(s) || Sizes.checkServerNameSize(s) || !members.containsKey(s)))
            .findAny();
        if (invalidServer.isPresent()) {
            logMessageAndClose(
                Level.SEVERE,
                "Invalid member (leader= "
                    + remoteName
                    + ") serverName ("
                    + invalidServer.get()
                    + "). Closing connection...",
                infos.address(),
                infos.connection()
            );
            return false;
        }

        return true;
    }


    private void fuse(
        String remoteName,
        List<String> remoteMembers,
        InetSocketAddress remoteAddress,
        UnknownRemoteInfo infos
    ) {
        if (!isFusing) {
            throw new IllegalStateException("Fusion is not in progress");
        }

        var other = infos.controller();
        var otherInfos = new IdentifiedRemoteInfo(remoteName, infos.connection(), remoteAddress);
        other.setVisitor(Visitors.fusedServerVisitor(server, otherInfos));

        isFusing = false;

        var stillLeader = serverName.compareTo(remoteName) < 0;
        if (stillLeader) {
            LOGGER.log(Level.INFO, "Still leader");
            members.put(remoteName, other);
            futureMembers.addAll(remoteMembers);
            return;
        }

        LOGGER.log(Level.INFO, remoteName + "(" + remoteAddress + ") is the new leader");
        leader = new ServerLeader(other, otherInfos);

        var buffer = Frame.FusionChangeLeader.buffer(remoteName, remoteAddress);
        this.members.values().forEach(c -> {
            c.queueData(buffer);
            c.closeWhenAllSent();
        });
        this.members = new HashMap<>();
    }

    public void sendToAllExcept(ByteBuffer data, String originServer) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(originServer);
        members.forEach((key, value) -> {
            if (originServer.equals(key)) return;
            value.queueData(BufferUtils.copy(data));
        });
    }

    public void initFusion(
        InetSocketAddress remote,
        Function<SocketChannel, SelectionKey> factory
    ) throws IOException {
        Objects.requireNonNull(remote);
        Objects.requireNonNull(factory);
        if (isFusing) {
            LOGGER.log(Level.WARNING, "Already fusing, ignoring...");
            return;
        }
        isFusing = true;

        if (leader != null) {
            LOGGER.log(Level.INFO, "Forwarding fusion request to leader");
            var data = Frame.FusionRequest.buffer(remote);
            leader.controller().queueData(data);
        } else {
            sendRequest(remote, factory);
        }
    }

    private void sendRequest(InetSocketAddress remote, Function<SocketChannel, SelectionKey> factory)
        throws IOException {
        LOGGER.log(Level.INFO, "Sending fusion request...");
        var sc = SocketChannel.open();
        sc.configureBlocking(false);
        try {
            sc.connect(remote);
        } catch (UnresolvedAddressException e) {
            LOGGER.log(Level.INFO, "Unknown address: " + remote);
            isFusing = false;
            return;
        }
        var key = factory.apply(sc);
        var ctx = new SelectionKeyControllerImpl(key, remote, false, true, false);
        var infos = new UnknownRemoteInfo(sc, remote, ctx);
        ctx.setVisitor(Visitors.pendingFusionVisitor(server, infos));
        ctx.setOnClose(() -> {
            LOGGER.log(Level.SEVERE, "Failed to connect to leader");
            isFusing = false;
        });
        key.attach(ctx);

        var data = Frame.FusionInit.buffer(serverName, address, new ArrayList<>(members.keySet()));
        ctx.queueData(data);
    }

    public String info() {
        var leaderInfo = "Leader = " + (
            leader != null
                ? leader.infos().name() + " (" + leader.infos().address() + ")\n"
                : "self\n");

        var size = members.size();
        if (size == 0) {
            return leaderInfo + size + " fused member.\n";
        }
        var connectedList = members.entrySet().stream()
            .map(e -> e.getKey() + " (" + e.getValue().remoteAddress() + ")")
            .collect(Collectors.joining("\n-"));
        return leaderInfo + size + " fused member(s):\n-" + connectedList;
    }

    public void changeLeader(
        Frame.FusionChangeLeader newLeader,
        IdentifiedRemoteInfo infos,
        Function<SocketChannel, SelectionKey> factory
    ) throws IOException {
        Objects.requireNonNull(newLeader);
        Objects.requireNonNull(infos);
        Objects.requireNonNull(factory);
        if (leader == null || !leader.infos().address().equals(infos.address())) {
            logMessageAndClose(Level.SEVERE, "Change leader without being leader", address, infos.connection());
            return;
        }

        LOGGER.log(
            Level.INFO,
            "Change leader to " + newLeader.leaderName() + "(" + newLeader.leaderAddress() + ")"
        );

        var sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(newLeader.leaderAddress());
        var key = factory.apply(sc);

        // creating the context and setting it as leader
        var ctx = new SelectionKeyControllerImpl(key, newLeader.leaderAddress(), false, true, false);
        var leaderInfos = new IdentifiedRemoteInfo(newLeader.leaderName(), sc, newLeader.leaderAddress());
        ctx.setVisitor(Visitors.fusedServerVisitor(server, leaderInfos));
        key.attach(ctx);
        leader = new ServerLeader(ctx, leaderInfos);

        var data = Frame.FusionMerge.buffer(serverName);
        ctx.queueData(data);
    }

    public void rejectFusion(Frame.FusionInitKo fusionInitKo, UnknownRemoteInfo infos) {
        Objects.requireNonNull(fusionInitKo);
        Objects.requireNonNull(infos);
        logMessageAndClose(
            Level.INFO,
            "Reject fusion request from " + infos.address(),
            infos.address(),
            infos.connection()
        );
        isFusing = false;
    }

    public boolean tryForwardPublicMessage(Frame.PublicMessage message, IdentifiedRemoteInfo infos) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(infos);
        if (
            leader != null
                && !leader.infos().name().equals(message.originServer())
                && !serverName.equals(message.originServer())
        ) {
            logMessageAndClose(
                Level.WARNING,
                "Received public message from "
                    + message.originServer()
                    + " but leader is "
                    + leader.infos().name(),
                infos.address(),
                infos.connection()
            );
            return false;
        }

        if (leader == null && members.isEmpty()) {
            return true;
        }

        if (leader == null) {
            sendToAllExcept(message.buffer(), message.originServer());
        } else {
            leader.controller().queueData(message.buffer());
        }
        return true;
    }

    public void forwardedFusion() {
        isFusing = false;
    }

    public boolean isLeader() {
        return leader == null;
    }

    public void forwardDirectMessage(Frame.DirectMessage message, IdentifiedRemoteInfo infos) {
        Objects.requireNonNull(message);
        Objects.requireNonNull(infos);

        var destinationServer = message.destinationServer();
        forwardData(destinationServer, message::buffer);
    }

    public void forwardFileSending(Frame.FileSending fileSending, IdentifiedRemoteInfo infos) {
        Objects.requireNonNull(fileSending);
        Objects.requireNonNull(infos);

        var destinationServer = fileSending.destinationServer();
        forwardData(destinationServer, fileSending::buffer);
    }

    private void forwardData(String destinationServer, Supplier<ByteBuffer> data) {
        if (leader != null) {
            leader.controller().queueData(data.get());
            return;
        }
        var recipient = members.get(destinationServer);
        if (recipient == null) {
            LOGGER.log(Level.INFO, "Direct message destination server (" + destinationServer + ") not found");
            return;
        }
        recipient.queueData(data.get());
    }

    private void logMessageAndClose(Level level, String message, InetSocketAddress address, Closeable closeable) {
        LOGGER.log(level, address + " : " + message);
        CloseableUtils.silentlyClose(closeable);
    }

    private record ServerLeader(SelectionKeyController controller, IdentifiedRemoteInfo infos) {
        public ServerLeader {
            Objects.requireNonNull(controller);
            Objects.requireNonNull(infos);
        }
    }
}
