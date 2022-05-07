package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.base.BufferUtils;
import fr.uge.chatfusion.core.frame.FrameVisitor;
import fr.uge.chatfusion.core.selection.SelectionKeyController;
import fr.uge.chatfusion.core.selection.SelectionKeyControllerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Objects;

final class ClientKeyController implements SelectionKeyController {
    private final ArrayDeque<ByteBuffer> messageQueue = new ArrayDeque<>();
    private final FileSendingController fileSendingController = new FileSendingController();
    private final SelectionKeyControllerImpl inner;

    public ClientKeyController(SelectionKey key, InetSocketAddress remoteAddress) {
        inner = new SelectionKeyControllerImpl(key, remoteAddress, false, false, true);
        inner.setOnSendingAllData(this::processOut);
    }

    @Override
    public void doRead() throws IOException {
        inner.doRead();
    }

    @Override
    public void doWrite() throws IOException {
        inner.doWrite();
    }

    @Override
    public void doConnect() throws IOException {
        inner.doConnect();
    }

    @Override
    public void queueData(ByteBuffer data) {
        Objects.requireNonNull(data);
        messageQueue.add(BufferUtils.copy(data));
        processOut();
    }

    @Override
    public void closeWhenAllSent() {
        inner.closeWhenAllSent();
    }

    @Override
    public void close() {
        inner.close();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return inner.remoteAddress();
    }

    public void queueFile(String originSrv, String sender, String dstSrv, String dstUser, Path filePath) {
        Objects.requireNonNull(originSrv);
        Objects.requireNonNull(sender);
        Objects.requireNonNull(dstSrv);
        Objects.requireNonNull(dstUser);
        Objects.requireNonNull(filePath);
        fileSendingController.sendFile(originSrv, sender, dstSrv, dstUser, filePath);
        processOut();
    }

    public void setVisitor(FrameVisitor visitor) {
        inner.setVisitor(visitor);
    }

    public void setOnClose(Runnable onClose) {
        inner.setOnClose(onClose);
    }

    private void processOut() {
        while (!messageQueue.isEmpty()) {
            inner.queueData(messageQueue.pop());
        }
        fileSendingController.nextFileSendingFrame().ifPresent(inner::queueData);
    }
}
