package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.Frame;

public interface UnknownToServerInterface {

    void connectAnonymously(Frame.AnonymousLogin anonymousLogin, UnknownRemoteInfo infos);

    void tryFusion(Frame.FusionInit fusionInit, UnknownRemoteInfo infos);

    void fusionMerge(Frame.FusionMerge fusionMerge, UnknownRemoteInfo infos);

    void fusionAccepted(Frame.FusionInitOk fusionInitOk, UnknownRemoteInfo infos);

    void fusionRejected(Frame.FusionInitKo fusionInitKo, UnknownRemoteInfo infos);
}
