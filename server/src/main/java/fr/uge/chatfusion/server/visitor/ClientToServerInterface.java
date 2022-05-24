package fr.uge.chatfusion.server.visitor;

import fr.uge.chatfusion.core.frame.Frame;

public interface ClientToServerInterface {

    void sendPublicMessage(Frame.PublicMessage message, IdentifiedRemoteInfo infos);

    void sendDirectMessage(Frame.DirectMessage message, IdentifiedRemoteInfo infos);

    void sendFile(Frame.FileSending file, IdentifiedRemoteInfo infos);
}
