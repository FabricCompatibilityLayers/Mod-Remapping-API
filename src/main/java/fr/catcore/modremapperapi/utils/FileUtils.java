package fr.catcore.modremapperapi.utils;

import net.fabricmc.loader.impl.launch.FabricLauncherBase;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@Deprecated
public class FileUtils {
    @Deprecated
    public static void writeTextFile(Collection<String> lines, File file) {
        file.getParentFile().mkdirs();
        
        try {
            Files.writeString(file.toPath(), String.join(System.lineSeparator(), lines));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    public static List<String> readTextSource(String path) {        
        try {
            var url = FabricLauncherBase.class.getClassLoader().getResource(path);

            if (url != null) {
                return Files.readAllLines(Path.of(url.toURI()));
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        return List.of();
    }

    @Deprecated
    public static void excludeFromZipFile(File file, List<String> excluded) throws IOException {
        try {
            io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.FileUtils.removeEntriesFromZip(file.toPath(), excluded);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public static void copyFile(Path original, Path copy) throws IOException {
        io.github.fabriccompatibilitylayers.modremappingapi.impl.utils.FileUtils.copyZipFile(original, copy);
    }
}
