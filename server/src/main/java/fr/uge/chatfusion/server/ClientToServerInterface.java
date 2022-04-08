package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.frame.PublicMessage;

import java.nio.channels.SelectionKey;

public interface ClientToServerInterface {

    String name();

    void tryToConnectAnonymously(String login, SelectionKey key);

    void sendPublicMessage(PublicMessage message);

    void disconnectClient(String username);

}
