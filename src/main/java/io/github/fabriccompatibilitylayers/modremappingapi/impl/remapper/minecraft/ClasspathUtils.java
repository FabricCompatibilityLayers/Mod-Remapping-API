package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.minecraft;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ObjectShare;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class ClasspathUtils {
    public static List<Path> getRemapClasspath() throws IOException {
        String remapClasspathFile = System.getProperty("fabric.remapClasspathFile");

        if (remapClasspathFile == null) {
            System.out.println("remapClasspathFile is null! Falling back to ObjectShare.");
            return getClassPathFromObjectShare();
        }

        String content = new String(Files.readAllBytes(Paths.get(remapClasspathFile)), StandardCharsets.UTF_8);

        return Arrays.stream(content.split(File.pathSeparator))
                .map(Paths::get)
                .collect(Collectors.toList());
    }

    public static @NotNull List<Path> getClassPathFromObjectShare() {
        ObjectShare share = FabricLoader.getInstance().getObjectShare();
        Object inputs = share.get("fabric-loader:inputGameJars");
        List<Path> list = new ArrayList<>();

        Object oldJar = FabricLoader.getInstance().getObjectShare().get("fabric-loader:inputGameJar");

        List<Path> classPaths = FabricLauncherBase.getLauncher().getClassPath();

        if (inputs instanceof List) {
            List<Path> paths = (List<Path>) inputs;

            if (oldJar instanceof Path) {
                if (paths.get(0).toString().equals(oldJar.toString())) {
                    list.addAll(paths);
                } else {
                    list.add((Path) oldJar);
                }
            } else {
                list.addAll(paths);
            }
        } else {
            list.add((Path) oldJar);
        }

        list.addAll(classPaths);

        Object realmsJar = share.get("fabric-loader:inputRealmsJar");

        if (realmsJar instanceof Path) list.add((Path) realmsJar);

        return list;
    }
}
