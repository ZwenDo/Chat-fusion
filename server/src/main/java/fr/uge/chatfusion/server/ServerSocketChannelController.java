package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.CloseableUtils;
import fr.uge.chatfusion.core.selection.SelectionKeyController;
import fr.uge.chatfusion.core.selection.SelectionKeyControllerImpl;
import fr.uge.chatfusion.server.visitor.UnknownRemoteInfo;
import fr.uge.chatfusion.server.visitor.Visitors;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ServerSocketChannelController {
    private final Logger LOGGER = Logger.getLogger(ServerSocketChannelController.class.getName());

    private final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    private final ArrayDeque<Runnable> commands = new ArrayDeque<>();
    private final Selector selector;
    private final Server server;

    public ServerSocketChannelController(Server server, InetSocketAddress address, Selector selector) throws IOException {
        Objects.requireNonNull(server);
        Objects.requireNonNull(address);
        Objects.requireNonNull(selector);
        this.server = server;
        serverSocketChannel.bind(address);
        this.selector = selector;
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (!Thread.interrupted()) {
            try {
                selector.select(this::treatKey);
                processCommands();
            } catch (CancelledKeyException e) {
                // ignore exception cause by closing
            } catch (ClosedSelectorException e) {
                // ignore exception caused by server shutdown
                break;
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
    }

    public void addCommand(Runnable command) {
        Objects.requireNonNull(command);
        synchronized (commands) {
            commands.addLast(command);
            selector.wakeup();
        }
    }

    public void shutdown() {
        CloseableUtils.silentlyClose(serverSocketChannel);
    }

    private void processCommands() {
        synchronized (commands) {
            while (!commands.isEmpty()) {
                commands.removeFirst().run();
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
            LOGGER.log(Level.INFO, "Connection closed due to IOException", e);
            ((SelectionKeyController) key.attachment()).close();
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
        var ssc = (ServerSocketChannel) key.channel();
        var sc = ssc.accept();
        if (sc == null) return;

        sc.configureBlocking(false);
        var skey = sc.register(selector, SelectionKey.OP_READ);
        var remoteAddress = (InetSocketAddress) sc.getRemoteAddress();
        var controller = new SelectionKeyControllerImpl(
            skey,
            remoteAddress,
            true,
            true,
            false
        );
        var infos = new UnknownRemoteInfo(sc, remoteAddress, controller);
        var visitor = Visitors.defaultVisitor(server, infos);
        controller.setVisitor(visitor);
        skey.attach(controller);
    }
}
