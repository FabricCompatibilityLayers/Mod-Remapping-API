package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import fr.catcore.modremapperapi.utils.Constants;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import fr.catcore.modremapperapi.remapping.RemapUtil;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.compatibility.V0ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.CacheUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModDiscoverer {
    private static final Map<String, List<String>> EXCLUDED = new HashMap<>();

    protected static void init(List<ModRemapper> modRemappers, boolean remapClassEdits) {
        RemapUtil.init(modRemappers);

        List<ModEntry> mods = new ArrayList<>();

        for (ModRemapper remapper : modRemappers) {
            if (remapper instanceof V0ModRemapper) {
                EXCLUDED.putAll(((V0ModRemapper) remapper).getExclusions());
            }
        }

        for (ModRemapper remapper : modRemappers) {
            for (String jarFolder : remapper.getJarFolders()) {
                Path mcSubFolder = FabricLoader.getInstance().getGameDir().resolve(jarFolder);
                Path cacheFolder = CacheUtils.getCachePath(jarFolder);

                try {
                    if (!Files.exists(mcSubFolder)) Files.createDirectories(mcSubFolder);
                    if (!Files.exists(cacheFolder)) Files.createDirectories(cacheFolder);
                    else io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils.emptyDir(cacheFolder);

                    mods.addAll(discoverModsInFolder(mcSubFolder.toFile(), cacheFolder.toFile()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        File mainTempDir = CacheUtils.getCachePath("temp").toFile();
        if (mainTempDir.exists()) {
            io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils.emptyDir(mainTempDir.toPath());
        }

        Map<Path, Path> modPaths = mods.stream()
                .filter(entry -> entry.original != null)
                .collect(Collectors.toMap(entry -> entry.original.toPath(), entry -> entry.file.toPath()));

        if (!remapClassEdits) {
            modPaths = excludeClassEdits(modPaths);
        }

        for (Path path : modPaths.keySet()) {
            RemapUtil.makeModMappings(path);
        }

        RemapUtil.generateModMappings();

        RemapUtil.remapMods(modPaths);

        modPaths.values().forEach(FabricLauncherBase.getLauncher()::addToClassPath);
    }

    private static Map<Path, Path> excludeClassEdits(Map<Path, Path> modPaths) {
        Map<Path, Path> map = new HashMap<>();
        Map<Path, Path> convertMap = new HashMap<>();

        File mainTempDir = CacheUtils.getCachePath("temp").toFile();
        mainTempDir.mkdirs();


        for (Map.Entry<Path, Path> entry : modPaths.entrySet()) {
            File tempDir = new File(mainTempDir, entry.getValue().toFile().getParentFile().getName());
            if (!tempDir.exists()) tempDir.mkdir();

            File tempFile = new File(tempDir, entry.getValue().toFile().getName());
            map.put(tempFile.toPath(), entry.getValue());
            convertMap.put(entry.getKey(), tempFile.toPath());
        }

        List<Path> errored = new ArrayList<>();

        for (Map.Entry<Path, Path> entry : convertMap.entrySet()) {
            try {
                if (Files.isDirectory(entry.getKey())) {
                    io.github.fabriccompatibiltylayers.modremappingapi.impl.utils.FileUtils.zipFolder(entry.getKey(), entry.getValue());
                } else {
                    Files.copy(entry.getKey(), entry.getValue(), StandardCopyOption.REPLACE_EXISTING);
                }

                FileUtils.removeEntriesFromZip(entry.getValue(), RemapUtil.MC_CLASS_NAMES);
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                errored.add(entry.getValue());
            }
        }

        errored.forEach(map::remove);

        return map;
    }

    private static List<ModEntry> discoverModsInFolder(File folder, File destination) {
        List<ModEntry> mods = new ArrayList<>();

        File[] folderFiles = folder.listFiles();
        if (!folder.isDirectory() || folderFiles == null) return ImmutableList.of();

        for (File file : folderFiles) {
            String name = file.getName().replace(" ", "_");
            if (file.isDirectory() || (file.isFile() && (name.endsWith(".jar") || name.endsWith(".zip")))) {
                File remappedFile = new File(destination, name);

                List<ModEntry> modName = new ArrayList<>();

                boolean hasClass = false;
                boolean fabric = false;

                File[] fileFiles = file.listFiles();

                if (file.isDirectory() && fileFiles != null) {
                    remappedFile = new File(destination, name + ".zip");
                    for (File subFile : fileFiles) {
                        String subName = subFile.getName();
                        if (subFile.isFile()) {
                            if (subName.endsWith(".class")) {
                                hasClass = true;
                            }
                        }
                    }

                    if (/* modName.isEmpty() && */ hasClass) {
                        modName.add(new DefaultModEntry(
                                name.replace(".zip", "").replace(".jar", ""),
                                remappedFile,
                                file
                        ));
                    }

                    if (!modName.isEmpty() && EXCLUDED.containsKey(modName.get(0).modName)) {
                        for (String excluded :
                                EXCLUDED.get(modName.get(0).modName)) {
                            File excludedFile = new File(file, excluded);
                            if (excludedFile.delete()) {
                                Constants.MAIN_LOGGER.debug("File deleted: " + excludedFile.getName());
                            }
                        }
                    }
                } else {
                    try {
                        FileInputStream fileinputstream = new FileInputStream(file);
                        ZipInputStream zipinputstream = new ZipInputStream(fileinputstream);
                        while (true) {
                            ZipEntry zipentry = zipinputstream.getNextEntry();
                            if (zipentry == null) {
                                zipinputstream.close();
                                fileinputstream.close();
                                break;
                            }

                            String s1 = zipentry.getName();
                            String[] ss = s1.split("/");
                            String s2 = ss[ss.length - 1];
                            if (!zipentry.isDirectory()) {
                                if (s2.equals("fabric.mod.json")) {
//                                  modName.clear();
                                    fabric = true;
                                    break;
                                } else if (s2.endsWith(".class")) {
                                    hasClass = true;
                                }
                            }
                        }

                        if (/* modName.isEmpty() && */ hasClass && !fabric) {
                            modName.add(new DefaultModEntry(
                                    name.replace(".zip", "").replace(".jar", ""),
                                    remappedFile,
                                    file
                            ));
                        }

                        if (!modName.isEmpty()) {
                            if (EXCLUDED.containsKey(modName.get(0).modName)) {
                                FileUtils.removeEntriesFromZip(file.toPath(), EXCLUDED.get(modName.get(0).modName));
                            }
                        }
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }

                mods.addAll(modName);
            }
        }

        return mods;
    }
}
