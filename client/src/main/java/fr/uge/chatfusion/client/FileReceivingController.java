package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.frame.Frame;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Objects;

final class FileReceivingController {
    private final Path filePath;
    private final HashMap<Long, FileData> files = new HashMap<>();

    public FileReceivingController(Path filePath) {
        Objects.requireNonNull(filePath);
        this.filePath = filePath;
    }

    private static String insertBeforeExtension(String fileName, String toInsert) {
        var firstDot = fileName.indexOf('.', 1);
        if (firstDot == -1) {
            return fileName + toInsert;
        }
        var name = fileName.substring(0, firstDot);
        var extension = fileName.substring(firstDot);
        return name + toInsert + extension;
    }

    public void receiveFileBlock(Frame.FileSending fileSending) throws IOException {
        Objects.requireNonNull(fileSending);
        var data = files.compute(
            fileSending.fileId(),
            (__, old) -> {
                if (old != null) {
                    if (!old.originalName().equals(fileSending.fileName())) {
                        throw new IllegalStateException("Cannot receive two different files with the same id");
                    }
                    return old;
                }
                return createFileData(fileSending);
            }
        );

        data.receiveBlock(fileSending);

        if (data.isComplete()) {
            files.remove(fileSending.fileId());
            createFinalFile(data);
        }
    }

    private FileData createFileData(Frame.FileSending fileSending) {
        System.out.println("Start receiving file " + fileSending.fileName() + " ...");
        var fileName = fileSending.fileName();
        var file = Path.of(filePath.toString(), fileName + ".tmp");
        var index = 1;
        while (file.toFile().exists()) { // add an index in case of name collision
            file = Path.of(filePath.toString(), fileName + "(" + index + ").tmp");
            index++;
        }
        return new FileData(fileName, file, fileSending.blockCount());
    }

    private void createFinalFile(FileData data) throws IOException {
        var finalName = data.originalName();
        var file = data.file();
        var finalFile = file.resolveSibling(finalName);
        var index = 1;
        while (finalFile.toFile().exists()) { // add an index in case of name collision
            var name = insertBeforeExtension(finalName,  "(" + index + ")");
            finalFile = file.resolveSibling(name);
            index++;
        }
        System.out.println("Received file " + finalName);
        Files.move(file, finalFile);
    }

    private static final class FileData {
        private final Path file;
        private final String originalName;
        private int missingBlocks;

        public FileData(String originalName, Path file, int missingBlocks) {
            Objects.requireNonNull(originalName);
            Objects.requireNonNull(file);
            if (missingBlocks < 0) {
                throw new IllegalArgumentException("missingBlocks must be positive");
            }
            this.originalName = originalName;
            this.file = file;
            this.missingBlocks = missingBlocks;
        }

        public void receiveBlock(Frame.FileSending fileSending) throws IOException {
            Objects.requireNonNull(fileSending);
            if (missingBlocks == 0) {
                throw new IllegalStateException("File already received");
            }
            try (var fileChannel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                var buffer = fileSending.block();
                buffer.flip();
                fileChannel.write(buffer);
            }
            missingBlocks--;
        }

        public boolean isComplete() {
            return missingBlocks == 0;
        }

        public Path file() {
            return file;
        }

        public String originalName() {
            return originalName;
        }
    }
}
