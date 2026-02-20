package io.github.fabriccompatibilitylayers.modremappingapi.impl.utils;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.SimpleFileVisitor;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@ApiStatus.Internal
public class FileUtils {

    @ApiStatus.Internal
    public static boolean exist(Collection<Path> paths) {
        for (Path path : paths) {
            if (!Files.exists(path)) return false;
        }

        return true;
    }

    @ApiStatus.Internal
    public static void delete(Collection<Path> paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @ApiStatus.Internal
    public static void downloadFile(String url, Path target) throws IOException {
        try (var inputStream = new BufferedInputStream(new URL(url).openStream());
             var outputStream = new BufferedOutputStream(Files.newOutputStream(target))) {
            
            var buffer = new byte[2048];
    
            // Increments file size
            int length;
            int downloaded = 0;
    
            // Looping until server finishes
            while ((length = inputStream.read(buffer)) != -1) {
                // Writing data
                outputStream.write(buffer, 0, length);
                downloaded += length;
            }
        }
    }

    @ApiStatus.Internal
    public static void copyZipFile(Path original, Path target) throws IOException {
        Files.deleteIfExists(target);
    
        try (var zin = new ZipInputStream(Files.newInputStream(original));
             var zout = new ZipOutputStream(Files.newOutputStream(target))) {
    
            var buf = new byte[1024];
            ZipEntry entry;
    
            while ((entry = zin.getNextEntry()) != null) {
                var zipEntryName = entry.getName();
    
                zout.putNextEntry(new ZipEntry(zipEntryName));
                // Transfer bytes from the ZIP file to the output file
                int len;
                while ((len = zin.read(buf)) > 0) {
                    zout.write(buf, 0, len);
                }
            }
        }
    }

    @ApiStatus.Internal
    public static List<String> listZipContent(Path path) throws IOException {
        var files = new ArrayList<String>();
    
        try (var zin = new ZipInputStream(Files.newInputStream(path))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    files.add(entry.getName().replace("\\", "/"));
                }
            }
        }

        return files;
    }

    /**
     * @author moehreag
     */
    @ApiStatus.Internal
    public static List<String> listPathContent(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var s = Files.walk(path)) {
                return s.map(Path::toString).collect(Collectors.toCollection(ArrayList::new));
            }
        } else if (Files.isRegularFile(path)) {
            return listZipContent(path);
        }

        return new ArrayList<>();
    }

    @ApiStatus.Internal
    public static void emptyDir(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public @NotNull FileVisitResult postVisitDirectory(@NotNull Path subDir, IOException exc) throws IOException {
                    if (!dir.equals(subDir)) {
                        Files.delete(subDir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @author moehreag
     */
    @ApiStatus.Internal
    public static void zipFolder(Path in, Path out) throws IOException {
        try (var outFs = FileSystems.newFileSystem(out)) {
            Files.walkFileTree(in, new SimpleFileVisitor<>(){
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    var outPath = outFs.getPath(in.relativize(file).toString());
                    Files.createDirectories(outPath.getParent());
                    Files.copy(file, outPath);
                    return super.visitFile(file, attrs);
                }
            });
        }
    }

    /* Define ZIP File System Properties in Map */
    private static final Map<String, String> ZIP_PROPERTIES = Map.of(
        /* We want to read an existing ZIP File, so we set this to False */
        "create", "false",
        /* Specify the encoding as UTF-8 */
        "encoding", "UTF-8"
    );
    
    @ApiStatus.Internal
    public static FileSystem getJarFileSystem(Path path) throws IOException {
        return FileSystems.newFileSystem(path, ZIP_PROPERTIES);
    }
    
    @ApiStatus.Internal
    public static void removeEntriesFromZip(Path zipPath, List<String> entries) throws IOException, URISyntaxException {
        try (var zipfs = getJarFileSystem(zipPath)) {
            for (var entryName : entries) {
                var entryPath = zipfs.getPath(entryName);

                if (Files.exists(entryPath)) {
                    Files.delete(entryPath);
                }
            }
        }
    }
}
