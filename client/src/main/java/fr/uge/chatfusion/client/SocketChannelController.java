package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.selection.SelectionKeyController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Consumer;

final class SocketChannelController {
    private final ArrayDeque<Runnable> commands = new ArrayDeque<>(1);
    private final SocketChannel socketChannel;
    private final InetSocketAddress address;
    private final Selector selector;

    public SocketChannelController(SocketChannel socketChannel, InetSocketAddress address, Selector selector) {
        Objects.requireNonNull(socketChannel);
        Objects.requireNonNull(address);
        Objects.requireNonNull(selector);
        this.socketChannel = socketChannel;
        this.address = address;
        this.selector = selector;
    }

    public void launch(Client client, Consumer<UniqueContext> onCreation) throws IOException {
        socketChannel.configureBlocking(false);
        socketChannel.connect(address);
        var key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
        var ctx = new UniqueContext(client, key, address);
        onCreation.accept(ctx);
        key.attach(ctx);
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

    public void addCommand(Runnable command) {
        Objects.requireNonNull(command);
        synchronized (commands) {
            commands.addLast(command);
            selector.wakeup();
        }
    }

    private void treatKey(SelectionKey key) {
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
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}
