package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.frame.*;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;

public interface ServerToServerInterface {

    String name();

    void tryFusion(Frame.FusionInit fusionInit, SelectionKey key);

    void mergeFusion(Frame.FusionMerge fusionMerge, SelectionKey key);

    InetSocketAddress leaderAddress();

    void acceptFusion(Frame.FusionInitOk fusionInitOk, SelectionKey key);

    void forwardPublicMessage(Frame.PublicMessage message);

    void fusionRequest(Frame.FusionRequest fusionRequest, SelectionKey key, InetSocketAddress address);

    void changeLeader(Frame.FusionChangeLeader changeLeader, SelectionKey key, InetSocketAddress address);

    void rejectFusion(Frame.FusionInitKo fusionInitKo, SelectionKey key, InetSocketAddress address);
}
