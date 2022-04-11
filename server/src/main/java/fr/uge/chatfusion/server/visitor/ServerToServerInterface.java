package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.Frame;

public interface ServerToServerInterface {
    void forwardPublicMessage(Frame.PublicMessage message, IdentifiedRemoteInfo infos);

    void fusionRequest(Frame.FusionRequest fusionRequest, IdentifiedRemoteInfo infos);

    void changeLeader(Frame.FusionChangeLeader changeLeader, IdentifiedRemoteInfo infos);
}
