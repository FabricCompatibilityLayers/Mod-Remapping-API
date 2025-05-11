package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.minecraft;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ObjectShare;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ApiStatus.Internal
public class ClasspathUtils {
    public static List<Path> getRemapClasspath() throws IOException {
        var remapClasspathFile = System.getProperty("fabric.remapClasspathFile");

        if (remapClasspathFile == null) {
            System.out.println("remapClasspathFile is null! Falling back to ObjectShare.");
            return getClassPathFromObjectShare();
        }

        var content = Files.readString(Path.of(remapClasspathFile));

        return Arrays.stream(content.split(File.pathSeparator))
                .map(Path::of)
                .toList();
    }

    public static @NotNull List<Path> getClassPathFromObjectShare() {
        var share = FabricLoader.getInstance().getObjectShare();
        var inputs = share.get("fabric-loader:inputGameJars");
        var list = new ArrayList<Path>();

        var oldJar = FabricLoader.getInstance().getObjectShare().get("fabric-loader:inputGameJar");
        var classPaths = FabricLauncherBase.getLauncher().getClassPath();

        if (inputs instanceof List<?> inputsList) {
            var paths = (List<Path>) inputsList;

            if (oldJar instanceof Path oldJarPath) {
                if (paths.get(0).toString().equals(oldJarPath.toString())) {
                    list.addAll(paths);
                } else {
                    list.add(oldJarPath);
                }
            } else {
                list.addAll(paths);
            }
        } else if (oldJar instanceof Path oldJarPath) {
            list.add(oldJarPath);
        }

        list.addAll(classPaths);

        Optional.ofNullable(share.get("fabric-loader:inputRealmsJar"))
                .filter(Path.class::isInstance)
                .map(Path.class::cast)
                .ifPresent(list::add);

        return list;
    }
}
