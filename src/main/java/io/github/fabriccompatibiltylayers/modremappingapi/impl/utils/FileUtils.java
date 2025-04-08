package io.github.fabriccompatibiltylayers.modremappingapi.impl.utils;

import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
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
        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream())) {
            try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(target))) {
                byte[] buffer = new byte[2048];

                // Increments file size
                int length;
                int downloaded = 0;

                // Looping until server finishes
                while ((length = inputStream.read(buffer)) != -1) {
                    // Writing data
                    outputStream.write(buffer, 0, length);
                    downloaded += length;
//                                    Constants.MAIN_LOGGER.debug("Download Status: " + (downloaded * 100) / (contentLength * 1.0) + "%");
                }

                outputStream.close();
                inputStream.close();
            }
        }
    }

    @ApiStatus.Internal
    public static void copyZipFile(Path original, Path target) throws IOException {
        target.toFile().delete();

        ZipInputStream zin = new ZipInputStream(Files.newInputStream(original));
        ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(target));

        ZipEntry entry = zin.getNextEntry();
        byte[] buf = new byte[1024];

        while (entry != null) {
            String zipEntryName = entry.getName();

            zout.putNextEntry(new ZipEntry(zipEntryName));
            // Transfer bytes from the ZIP file to the output file
            int len;
            while ((len = zin.read(buf)) > 0) {
                zout.write(buf, 0, len);
            }

            entry = zin.getNextEntry();
        }

        // Close the streams
        zin.close();
        // Compress the files
        // Complete the ZIP file
        zout.close();
    }

    @ApiStatus.Internal
    public static List<String> listZipContent(Path path) throws IOException {
        List<String> files = new ArrayList<>();

        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(path))) {
            while (true) {
                ZipEntry entry = zin.getNextEntry();
                if (entry == null) {
                    break;
                }

                String name = entry.getName();
                if (!entry.isDirectory()) {
                    files.add(name.replace("\\", "/"));
                }
            }
        }

        return files;
    }

    @ApiStatus.Internal
    public static List<String> listDirectoryContent(File[] files) {
        List<String> list = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();

                for (String fileName : listDirectoryContent(file.listFiles())) {
                    list.add(name + "/" + fileName);
                }
            } else if (file.isFile()) {
                list.add(file.getName());
            }
        }

        return list;
    }

    @ApiStatus.Internal
    public static List<String> listPathContent(Path path) throws IOException {
        File file = path.toFile();

        if (file.isDirectory()) {
            return listDirectoryContent(file.listFiles());
        } else if (file.isFile()) {
            return listZipContent(path);
        }

        return new ArrayList<>();
    }

    @ApiStatus.Internal
    public static void emptyDir(Path dir) {
        try {
            Files.walkFileTree(dir, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path subDir, IOException exc) throws IOException {
                    if (dir != subDir) Files.delete(subDir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ApiStatus.Internal
    public static void zipFolder(Path input, Path output) throws IOException {
        try (ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(output))) {
            File fileToZip = input.toFile();

            zipFile(fileToZip, fileToZip.getName(), zout, true);
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut, boolean root) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }

        if (fileToZip.isDirectory()) {
            if (!root) {
                if (fileName.endsWith("/")) {
                    zipOut.putNextEntry(new ZipEntry(fileName));
                    zipOut.closeEntry();
                } else {
                    zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                    zipOut.closeEntry();
                }
            }

            File[] children = fileToZip.listFiles();

            for (File childFile : children) {
                zipFile(childFile, (root ? "" : fileName + "/") + childFile.getName(), zipOut, false);
            }

            return;
        }

        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            byte[] bytes = new byte[1024];
            int length;

            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }

        zipOut.closeEntry();
    }

    /* Define ZIP File System Properies in HashMap */
    private static final Map<String, String> ZIP_PROPERTIES = new HashMap<>();

    static {
        /* We want to read an existing ZIP File, so we set this to False */
        ZIP_PROPERTIES.put("create", "false");
        /* Specify the encoding as UTF-8 */
        ZIP_PROPERTIES.put("encoding", "UTF-8");
    }

    @ApiStatus.Internal
    public static FileSystem getJarFileSystem(Path path) throws URISyntaxException, IOException {
        return FileSystems.newFileSystem(URI.create("jar:" + path.toUri()), ZIP_PROPERTIES);
    }

    @ApiStatus.Internal
    public static void removeEntriesFromZip(Path zipPath, List<String> entries) throws IOException, URISyntaxException {
        try (FileSystem zipfs = getJarFileSystem(zipPath)) {
            for (String entryName : entries) {
                Path entryPath = zipfs.getPath(entryName);

                if (Files.exists(entryPath)) {
                    Files.delete(entryPath);
                }
            }
        }
    }
}
