package fr.catcore.modremapperapi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {
    public static void emptyFolder(Path path) throws IOException {
        Files.walkFileTree(path, new FileVisitor<Path>() {
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
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static FileSystem getJarFS(Path jar) throws URISyntaxException, IOException {
        /* Define ZIP File System Properies in HashMap */
        Map<String, String> zip_properties = new HashMap<>();
        /* We want to read an existing ZIP File, so we set this to False */
        zip_properties.put("create", "false");
        /* Specify the encoding as UTF -8 */
        zip_properties.put("encoding", "UTF-8");

        return FileSystems.newFileSystem(new URI("jar:" + jar.toUri()), zip_properties);
    }

    public static void openZip(Path zip, ZipVisitor visitor) throws IOException {
        InputStream fileinputstream = Files.newInputStream(zip);
        ZipInputStream zipinputstream = new ZipInputStream(fileinputstream);

        while (true) {
            ZipEntry zipentry = zipinputstream.getNextEntry();
            if (zipentry == null) {
                break;
            }

            String s1 = zipentry.getName();
            String[] ss = s1.split("/");
            String s2 = ss[ss.length - 1];

            if (visitor.checkEntry(zipentry, s1, s2)) break;
        }

        zipinputstream.close();
        fileinputstream.close();
    }

    @FunctionalInterface
    public interface ZipVisitor {
        boolean checkEntry(ZipEntry entry, String fullName, String shortName);
    }
}
