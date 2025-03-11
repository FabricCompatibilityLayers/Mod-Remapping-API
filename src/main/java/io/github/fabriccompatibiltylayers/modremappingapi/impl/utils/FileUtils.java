package io.github.fabriccompatibiltylayers.modremappingapi.impl.utils;

import org.jetbrains.annotations.ApiStatus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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
}
