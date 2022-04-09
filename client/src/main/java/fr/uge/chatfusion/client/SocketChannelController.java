package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.CloseableUtils;
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

final class SocketChannelController {
    private final ArrayDeque<Runnable> commands = new ArrayDeque<>(1);
    private final Selector selector = Selector.open();
    private final SocketChannel socketChannel = SocketChannel.open();
    private final InetSocketAddress serverAddress;


    public SocketChannelController(
        InetSocketAddress serverAddress
    ) throws IOException {
        Objects.requireNonNull(serverAddress);
        this.serverAddress = serverAddress;
    }

    public SelectionKey createSelectionKey() throws IOException {
        socketChannel.configureBlocking(false);
        return socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    public void launch() throws IOException {
        socketChannel.connect(serverAddress);
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

    public void shutdown() {
        CloseableUtils.silentlyClose(selector);
    }
}
