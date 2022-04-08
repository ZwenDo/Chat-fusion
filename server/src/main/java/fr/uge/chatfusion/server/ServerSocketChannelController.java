package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.selection.SelectionKeyController;
import fr.uge.chatfusion.server.context.DefaultContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.BiConsumer;

final class ServerSocketChannelController {
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final Server server;
    private final BiConsumer<IOException, SelectionKey> onException;
    private final ArrayDeque<Runnable> commands = new ArrayDeque<>();

    public ServerSocketChannelController(
        Server server,
        ServerSocketChannel serverSocketChannel,
        Selector selector,
        BiConsumer<IOException, SelectionKey> onException
    ) throws IOException {
        Objects.requireNonNull(server);
        Objects.requireNonNull(serverSocketChannel);
        Objects.requireNonNull(selector);
        Objects.requireNonNull(onException);
        this.server = server;
        this.serverSocketChannel = serverSocketChannel;
        this.selector = selector;
        this.onException = onException;
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (!Thread.interrupted()) {
            try {
                selector.select(this::treatKey);
                processCommands();
            } catch (ClosedSelectorException e) {
                // ignore
                break;
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
    }

    private void processCommands() {
        synchronized (commands) {
            while (!commands.isEmpty()) {
                commands.removeFirst().run();
            }
        }
    }

    void addCommand(Runnable command) {
        Objects.requireNonNull(command);
        synchronized (commands) {
            commands.addLast(command);
            selector.wakeup();
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
            if (key.isValid() && key.isConnectable()) {
                ((SelectionKeyController) key.attachment()).doConnect();
            }
            if (key.isValid() && key.isWritable()) {
                ((SelectionKeyController) key.attachment()).doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                ((SelectionKeyController) key.attachment()).doRead();
            }
        } catch (IOException e) {
            onException.accept(e, key);
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
        var ssc = (ServerSocketChannel) key.channel();
        var sc = ssc.accept();
        if (sc == null) return;
        sc.configureBlocking(false);
        var skey = sc.register(selector, SelectionKey.OP_READ);
        var remoteAddress = (InetSocketAddress) sc.getRemoteAddress();
        skey.attach(new DefaultContext(server, skey, remoteAddress, true));
    }
}
