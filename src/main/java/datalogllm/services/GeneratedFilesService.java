package datalogllm.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GeneratedFilesService {

    private final Path generatedRoot;

    public GeneratedFilesService(Path generatedRoot) {
        this.generatedRoot = generatedRoot.normalize().toAbsolutePath();
    }

    public List<GeneratedFileInfo> listGeneratedFiles() throws IOException {
        if (!Files.exists(generatedRoot)) return List.of();

        List<GeneratedFileInfo> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(generatedRoot)) {
            stream
                    .filter(path -> !path.equals(generatedRoot))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> {
                        boolean isDirectory = Files.isDirectory(path);
                        String relative = generatedRoot.relativize(path).toString().replace("\\", "/");
                        String name = path.getFileName().toString();
                        long size = isDirectory ? 0L : safeFileSize(path);
                        files.add(new GeneratedFileInfo(name, relative, isDirectory, size));
                    });
        }
        return files;
    }

    public String readGeneratedFile(String relativePath) throws IOException {
        Path resolved = resolveSafe(relativePath);
        if (Files.isDirectory(resolved)) {
            throw new IllegalArgumentException("Path points to a directory.");
        }
        return Files.readString(resolved, StandardCharsets.UTF_8);
    }

    public byte[] buildGeneratedFilesZip() throws IOException {
        if (!Files.exists(generatedRoot)) return new byte[0];

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(baos)) {

            try (Stream<Path> stream = Files.walk(generatedRoot)) {
                stream
                        .filter(path -> !Files.isDirectory(path))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(path -> {
                            String zipEntryName = generatedRoot.relativize(path).toString().replace("\\", "/");
                            try {
                                zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
                                zipOutputStream.write(Files.readAllBytes(path));
                                zipOutputStream.closeEntry();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException ioException) throw ioException;
                throw e;
            }

            zipOutputStream.finish();
            return baos.toByteArray();
        }
    }

    private Path resolveSafe(String relativePath) {
        Path resolved = generatedRoot.resolve(relativePath).normalize().toAbsolutePath();
        if (!resolved.startsWith(generatedRoot)) {
            throw new IllegalArgumentException("Invalid path.");
        }
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("File not found.");
        }
        return resolved;
    }

    private long safeFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    public record GeneratedFileInfo(String name, String path, boolean directory, long size) {
    }
}
