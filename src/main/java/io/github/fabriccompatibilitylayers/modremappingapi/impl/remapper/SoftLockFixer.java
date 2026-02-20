package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@ApiStatus.Internal
public class SoftLockFixer {
    public static void preloadClasses() {
        List<Stream<String>> classes = new ArrayList<>();

        for (String[] entry : new String[][]{
                new String[] {"org/objectweb/asm/", "org/objectweb/asm/MethodVisitor"},
                new String[] {"org/objectweb/asm/tree/", "org/objectweb/asm/tree/MethodNode"},
                new String[] {"org/objectweb/asm/commons/", "org/objectweb/asm/commons/Remapper"},
                new String[] {"org/objectweb/asm/util/", "org/objectweb/asm/util/CheckClassAdapter"},
                new String[] {"org/objectweb/asm/tree/analysis/", "org/objectweb/asm/tree/analysis/Value"},
                new String[] {"", "io/github/fabriccompatibilitylayers/modremappingapi/impl/remapper/TrRemapperHelper"},
                new String[] {getLibPackageName("tinyremapper", ""), getLibPackageName("tinyremapper", "TinyRemapper")},
                new String[] {getLibPackageName("mappingio", ""), getLibPackageName("mappingio", "MappingReader")}
        }) {
            try {
                classes.add(getEntries(entry[0], entry[1]));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Stream<String> t = classes.stream().flatMap(s -> s);
        List<String> toCheck = new ArrayList<>(Arrays.asList(new String[] {
                "net.fabricmc.loader.impl.launch.FabricLauncher",
                "net.fabricmc.loader.impl.launch.FabricLauncherBase",
                "net.fabricmc.loader.api.ObjectShare",
                "javax.lang.model.SourceVersion",
                getLibClassName("tinyremapper", "extension.mixin.hard.ImprovedHardTargetMixinClassVisitor"),
                getLibClassName("tinyremapper", "extension.mixin.hard.ImprovedMixinAnnotationVisitor")
        }));
        toCheck.addAll(t.toList());

        for (String clazz : toCheck) {
            try {
                if (!clazz.startsWith("META-INF.versions")) Class.forName(clazz, false, cl);
            } catch (ClassNotFoundException | NoClassDefFoundError | UnsupportedClassVersionError e) {
                if (!clazz.startsWith("META-INF.versions")) throw new RuntimeException(e);
            }
        }
    }

    private static String getLibPackageName(String lib, String string) {
        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();
        boolean shouldDev = isDev && System.getProperty("fabric-mod-remapper-api.dev") == null;

        if (shouldDev) {
            return "net/fabricmc/" + lib + "/" + string;
        }

        return "io/github/fabriccompatibilitylayers/modremappingapi/impl/lib/" + lib + "/" + string;
    }

    private static String getLibClassName(String lib, String string) {
        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();
        boolean shouldDev = isDev && System.getProperty("fabric-mod-remapper-api.dev") == null;

        if (shouldDev) {
            return "net.fabricmc." + lib + "." + string;
        }

        return "io.github.fabriccompatibilitylayers.modremappingapi.impl.lib." + lib + "." + string;
    }

    private static final ClassLoader cl = FabricLauncherBase.getLauncher().getTargetClassLoader();

    private static Stream<String> getEntries(String pkg, String locatorClass) throws IOException {
        URL url = cl.getResource(locatorClass + ".class");

        URLConnection connection = url.openConnection();
        return connection instanceof JarURLConnection jarConnection ?
                jarConnection.getJarFile().stream()
                .filter(jarEntry -> jarEntry.getName().contains(pkg) && jarEntry.getName().endsWith(".class"))
                .map(jarEntry -> jarEntry.getName().replace("/", ".").replaceFirst(".class$", ""))
                : Stream.empty();
    }
}
