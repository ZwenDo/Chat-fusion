package fr.uge.chatfusion.server;

import java.net.InetSocketAddress;

public interface ServerInterface {

    boolean tryToConnect(String login);

    void sendPublicMessage(InetSocketAddress senderAddress, String senderUsername, String message);
}
