package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.frame.*;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;

public interface ServerToServerInterface {

    String name();

    void tryFusion(FusionInit fusionInit, SelectionKey key);

    void mergeFusion(FusionMerge fusionMerge, SelectionKey key);

    InetSocketAddress leaderAddress();

    void acceptFusion(FusionInitOk fusionInitOk, SelectionKey key);

    void forwardPublicMessage(PublicMessage message);

    void fusionRequest(FusionRequest fusionRequest, SelectionKey key, InetSocketAddress address);

    void changeLeader(FusionChangeLeader changeLeader, SelectionKey key, InetSocketAddress address);

    void rejectFusion(FusionInitKo fusionInitKo, SelectionKey key, InetSocketAddress address);
}
