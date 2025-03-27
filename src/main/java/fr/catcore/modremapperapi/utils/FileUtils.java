package fr.catcore.modremapperapi.utils;

import net.fabricmc.loader.impl.launch.FabricLauncherBase;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Deprecated
public class FileUtils {
    @Deprecated
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

    @Deprecated
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

    @Deprecated
    public static void excludeFromZipFile(File file, List<String> excluded) throws IOException {
        try {
            io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils.removeEntriesFromZip(file.toPath(), excluded);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public static void copyFile(Path original, Path copy) throws IOException {
        io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils.copyZipFile(original, copy);
    }
}
