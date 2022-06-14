package fr.catcore.modremapperapi.utils;

import net.fabricmc.loader.impl.launch.FabricLauncherBase;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    public static void writeTextFile(Collection<String> lines, File file) {
        file.getParentFile().mkdirs();
        try {
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for (String line : lines) {
                bufferedWriter.append(line);
                bufferedWriter.append("\n");
            }
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readTextSource(String path) {
        List<String> result = new ArrayList<>();
        try {
            InputStream stream = FabricLauncherBase.class.getClassLoader().getResourceAsStream(path);
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader bufferedWriter = new BufferedReader(streamReader);
            String line = bufferedWriter.readLine();
            while (line != null) {
                result.add(line);
                line = bufferedWriter.readLine();
            }
            bufferedWriter.close();
            streamReader.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void excludeFromZipFile(File file, List<String> excluded) throws IOException {
        File tempFile = new File(file.getAbsolutePath() + ".tmp");
        tempFile.delete();
        tempFile.deleteOnExit();

        boolean renameOk = file.renameTo(tempFile);
        if (!renameOk) {
            throw new RuntimeException("could not rename the file " + file.getAbsolutePath() + " to " + tempFile.getAbsolutePath());
        }

        ZipInputStream zin = new ZipInputStream(Files.newInputStream(tempFile.toPath()));
        ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(file.toPath()));

        ZipEntry entry = zin.getNextEntry();
        byte[] buf = new byte[1024];

        while (entry != null) {
            String zipEntryName = entry.getName();
            boolean toBeDeleted = excluded.contains(zipEntryName);

            if (!toBeDeleted) {
                zout.putNextEntry(new ZipEntry(zipEntryName));
                // Transfer bytes from the ZIP file to the output file
                int len;
                while ((len = zin.read(buf)) > 0) {
                    zout.write(buf, 0, len);
                }
            }

            entry = zin.getNextEntry();
        }

        // Close the streams
        zin.close();
        // Compress the files
        // Complete the ZIP file
        zout.close();
        tempFile.delete();
    }

    public static void copyFile(Path original, Path copy) throws IOException {
        copy.toFile().delete();

        ZipInputStream zin = new ZipInputStream(Files.newInputStream(original));
        ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(copy));

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
