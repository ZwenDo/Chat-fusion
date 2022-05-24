package fr.uge.chatfusion.client;


import fr.uge.chatfusion.core.base.Sizes;
import fr.uge.chatfusion.core.frame.Frame;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

final class FileSendingController {
    private final ArrayDeque<FileData> queuedFiles = new ArrayDeque<>();
    private FileData currentFile;
    private int leftToSend;

    public Optional<ByteBuffer> nextFileSendingFrame() {
        if (currentFile == null || leftToSend == 0) {
            if (queuedFiles.isEmpty()) {
                return Optional.empty();
            }
            currentFile = queuedFiles.pop();
            leftToSend = currentFile.blockCount();
        }

        byte[] data;
        try {
            data = currentFile.stream().readNBytes(Sizes.MAX_FILE_BLOCK_SIZE);
            //currentFile.stream().read(blockBuffer.array());
        } catch (IOException e) {
            System.out.println("Error while reading file " + currentFile.filePath());
            currentFile = null;
            return Optional.empty();
        }
        var blockBuffer = ByteBuffer.wrap(data).compact();
        // TODO System.out.println("Sending block " + (currentFile.blockCount() - leftToSend) + " of " + currentFile.blockCount());
        var buffer = currentFile.toFrame(blockBuffer);
        leftToSend--;
        return Optional.of(buffer);
    }

    public void sendFile(
        String originServer,
        String sender,
        String destinationServer,
        String recipient,
        Path filePath
    ) {
        Objects.requireNonNull(originServer);
        Objects.requireNonNull(sender);
        Objects.requireNonNull(destinationServer);
        Objects.requireNonNull(recipient);
        Objects.requireNonNull(filePath);
        var id = new Random().nextLong();
        try {
            var file = filePath.toFile();
            var stream = new FileInputStream(file);
            var size = file.length();
            var blockCount = 1 + (int) size / Sizes.MAX_FILE_BLOCK_SIZE;
            var data = new FileData(
                originServer,
                sender,
                destinationServer,
                recipient,
                filePath,
                id,
                blockCount,
                stream
            );
            queuedFiles.add(data);
        } catch (FileNotFoundException e) {
            System.out.println("File " + filePath + " does not exist");
        }
    }

    private record FileData(
        String originServer,
        String sender,
        String destinationServer,
        String recipient,
        Path filePath,
        long id,
        int blockCount,
        FileInputStream stream
    ) {
        public ByteBuffer toFrame(ByteBuffer block) {
            Objects.requireNonNull(block);
            return Frame.FileSending.buffer(
                originServer,
                sender,
                destinationServer,
                recipient,
                id,
                filePath.getFileName().toString(),
                blockCount,
                block
            );
        }
    }
}
