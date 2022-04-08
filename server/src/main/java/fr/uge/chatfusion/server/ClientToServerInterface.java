package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.frame.Frame;

import java.nio.channels.SelectionKey;

public interface ClientToServerInterface {

    String name();

    void tryToConnectAnonymously(String login, SelectionKey key);

    void sendPublicMessage(Frame.PublicMessage message);

    void disconnectClient(String username);

}
