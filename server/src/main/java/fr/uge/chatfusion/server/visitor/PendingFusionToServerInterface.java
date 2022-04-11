package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.Frame;

public interface PendingFusionToServerInterface {

    void fusionAccepted(Frame.FusionInitOk fusionInitOk, UnknownRemoteInfo infos);

    void fusionRejected(Frame.FusionInitKo fusionInitKo, UnknownRemoteInfo infos);

    void fusionForwarded(Frame.FusionInitFwd fusionInitFwd, UnknownRemoteInfo infos);
}
