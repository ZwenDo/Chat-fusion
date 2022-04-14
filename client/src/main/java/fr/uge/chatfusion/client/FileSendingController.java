package fr.uge.chatfusion.client;


import fr.uge.chatfusion.core.Sizes;
import fr.uge.chatfusion.core.frame.Frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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

        var blockBuffer = ByteBuffer.allocate(Sizes.MAX_FILE_BLOCK_SIZE);
        try {
            currentFile.channel().read(blockBuffer);
        } catch (IOException e) {
            System.out.println("Error while reading file " + currentFile.filePath());
            currentFile = null;
            return Optional.empty();
        }

        var buffer = Frame.FileSending.buffer(
            currentFile.originServer(),
            currentFile.sender(),
            currentFile.destinationServer(),
            currentFile.recipient(),
            currentFile.id(),
            currentFile.filePath().getFileName().toString(),
            currentFile.blockCount(),
            blockBuffer
        );
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
            var channel = Files.newByteChannel(filePath);
            var size = channel.size();
            if (size == 0) {
                System.out.println("File " + filePath + " is empty");
                return;
            }

            var blockCount = 1 + (int) size / Sizes.MAX_FILE_BLOCK_SIZE;
            var data = new FileData(
                originServer,
                sender,
                destinationServer,
                recipient,
                filePath,
                id,
                blockCount,
                channel
            );
            queuedFiles.add(data);
        } catch (NoSuchFileException e) {
            System.out.println("File " + filePath + " does not exist");
        } catch (IOException e) {
            System.out.println("Error while opening file " + filePath);
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
        SeekableByteChannel channel
    ) {
    }
}
