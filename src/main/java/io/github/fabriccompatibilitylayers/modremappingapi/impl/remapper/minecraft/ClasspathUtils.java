package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.minecraft;

import fr.catcore.modremapperapi.utils.Constants;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class ClasspathUtils {
    public static Set<Path> getRemapClasspath() throws IOException {
        var remapClasspathFile = System.getProperty("fabric.remapClasspathFile");

        if (remapClasspathFile == null) {
            Constants.MAIN_LOGGER.info("remapClasspathFile is null! Falling back to ObjectShare.");
            return getClassPathFromObjectShare();
        }

        var content = Files.readString(Path.of(remapClasspathFile));

        return Arrays.stream(content.split(File.pathSeparator))
                .map(Path::of)
                .collect(Collectors.toSet());
    }

    public static @NotNull Set<Path> getClassPathFromObjectShare() {
        var share = FabricLoader.getInstance().getObjectShare();
        var inputs = share.get("fabric-loader:inputGameJars");
        var completeClasspath = new HashSet<Path>();

        var oldJar = FabricLoader.getInstance().getObjectShare().get("fabric-loader:inputGameJar");
        var classPaths = FabricLauncherBase.getLauncher().getClassPath();

        if (inputs instanceof Collection<?> inputsList) {
            var paths = (Collection<Path>) inputsList;
            completeClasspath.addAll(paths);
        }

        if (oldJar instanceof Path oldJarPath) {
            completeClasspath.add(oldJarPath);
        }

        completeClasspath.addAll(classPaths);

        Optional.ofNullable(share.get("fabric-loader:inputRealmsJar"))
                .filter(Path.class::isInstance)
                .map(Path.class::cast)
                .ifPresent(completeClasspath::add);

        return completeClasspath;
    }
}
