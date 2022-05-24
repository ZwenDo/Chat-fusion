package fr.uge.chatfusion.client;

import fr.uge.chatfusion.core.base.CloseableUtils;
import fr.uge.chatfusion.core.frame.Frame;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static void printInformation(String content) {
        var message = DateTimeUtils.printWithDateTime(content);
        System.out.println(message);
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
        if (data == null) {
            System.out.println("Error while receiving file");
            return;
        }
        data.receiveBlock(fileSending);

        if (data.isComplete()) {
            files.remove(fileSending.fileId());
            createFinalFile(data);
            data.close();
        }
    }

    private FileData createFileData(Frame.FileSending fileSending) {
        printInformation("Start receiving file \"" + fileSending.fileName() + "\" ...");
        var fileName = fileSending.fileName();
        var file = Path.of(filePath.toString(), fileName + ".part");
        var index = 1;
        while (file.toFile().exists()) { // add an index in case of name collision
            file = Path.of(filePath.toString(), fileName + "(" + index + ").part");
            index++;
        }
        try {
            return new FileData(fileName, file, fileSending.blockCount());
        } catch (IOException e) {
            return null;
        }
    }

    private void createFinalFile(FileData data) throws IOException {
        var finalName = data.originalName();
        var file = data.file();
        var finalFile = file.resolveSibling(finalName);
        var index = 1;
        while (finalFile.toFile().exists()) { // add an index in case of name collision
            var name = insertBeforeExtension(finalName, "(" + index + ")");
            finalFile = file.resolveSibling(name);
            index++;
        }
        printInformation("Received file \"" + finalName + "\"");
        Files.move(file, finalFile);
    }

    public void stopReceiving(Frame.FileSending fileSending) {
        Objects.requireNonNull(fileSending);
        var data = files.remove(fileSending.fileId());
        if (data != null && !data.isComplete()) {
            try {
                data.file.toFile().delete();
            } catch (SecurityException e) {
                // ignore
            }
        }
    }

    private static final class FileData {
        private final Path file;
        private final String originalName;
        private int missingBlocks;
        private final FileOutputStream stream;

        public FileData(String originalName, Path file, int missingBlocks) throws IOException {
            Objects.requireNonNull(originalName);
            Objects.requireNonNull(file);
            if (missingBlocks < 0) {
                throw new IllegalArgumentException("missingBlocks must be positive");
            }
            this.originalName = originalName;
            this.file = file;
            this.missingBlocks = missingBlocks;
            var actualFile = file.toFile();
            actualFile.createNewFile();
            this.stream = new FileOutputStream(actualFile);
        }

        public void receiveBlock(Frame.FileSending fileSending) throws IOException {
            Objects.requireNonNull(fileSending);
            if (missingBlocks == 0) {
                throw new IllegalStateException("File already received");
            }
            var buffer = fileSending.block();
            // TODO System.out.println("Received block " + (fileSending.blockCount() - missingBlocks) + " of " + fileSending.blockCount());
            stream.write(buffer.array(), 0, buffer.limit());
            missingBlocks--;
        }

        public boolean isComplete() {
            return missingBlocks == 0;
        }

        public void close() {
            CloseableUtils.silentlyClose(stream);
        }

        public Path file() {
            return file;
        }

        public String originalName() {
            return originalName;
        }
    }
}
