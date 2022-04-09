package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.Frame;

public interface ServerToServerInterface {
    void forwardPublicMessage(Frame.PublicMessage message, RemoteInfo infos);

    void fusionRequest(Frame.FusionRequest fusionRequest, RemoteInfo infos);

    void changeLeader(Frame.FusionChangeLeader changeLeader, RemoteInfo infos);
}
