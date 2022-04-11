package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.Frame;

public interface DefaultToServerInterface {

    void connectAnonymously(Frame.AnonymousLogin anonymousLogin, UnknownRemoteInfo infos);

    void tryFusion(Frame.FusionInit fusionInit, UnknownRemoteInfo infos);

    void fusionMerge(Frame.FusionMerge fusionMerge, UnknownRemoteInfo infos);
}
