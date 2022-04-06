package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.Sizes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

final class Server implements ServerInterface {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector = Selector.open();
    private final HashMap<String, SocketChannel> clients = new HashMap<>();

    public Server(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (!Thread.interrupted()) {
            try {
                selector.select(this::treatKey);
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isAcceptable()) {
                doAccept(key);
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        try {
            if (key.isValid() && key.isWritable()) {
                ((ClientContext) key.attachment()).doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                ((ClientContext) key.attachment()).doRead();
            }
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Connection closed with client due to IOException", e);
            CloseableUtils.silentlyClose(key.channel());
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
        var ssc = (ServerSocketChannel) key.channel();
        var sc = ssc.accept();
        if (sc == null) return;
        sc.configureBlocking(false);
        var skey = sc.register(selector, SelectionKey.OP_READ);
        skey.attach(new ClientContext(this, skey));
    }

    @Override
    public boolean tryToConnect(String login) {
        Objects.requireNonNull(login);
        System.out.println("Connected: " + login);
        return true;
    }

    @Override
    public void sendPublicMessage(InetSocketAddress senderAddress, String senderUsername, String message) {
        Objects.requireNonNull(senderAddress);
        Objects.requireNonNull(senderUsername);
        Objects.requireNonNull(message);
        var sc = clients.get(senderUsername);

        if (sc == null) {
            LOGGER.log(Level.SEVERE, "Client not found: " + senderUsername);
            return;
        }

        if (!Sizes.checkMessageSize(message)) {
            logAndSilentlyClose(Level.WARNING, "Message too long", senderAddress, sc);
            return;
        }

        System.out.println(senderUsername + ": " + message);
    }

    private void logAndSilentlyClose(Level level, String message, InetSocketAddress origin, AutoCloseable closeable) {
        CloseableUtils.logForRemoteAndSilentlyClose(LOGGER, level, message, origin, closeable);
    }
}
