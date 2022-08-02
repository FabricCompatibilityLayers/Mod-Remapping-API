package fr.catcore.modremapperapi.utils;

import fr.catcore.modremapperapi.ModRemappingAPI;
import fr.catcore.modremapperapi.api.ModRemapper;
import fr.catcore.modremapperapi.remapping.RemapUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModDiscoverer {
    private static final Map<String, List<String>> EXCLUDED = new HashMap<>();

    protected static void init() {
        RemapUtil.init();

        List<ModEntry> mods = new ArrayList<>();

        for (ModRemapper remapper : ModRemappingAPI.MOD_REMAPPERS) {
            EXCLUDED.putAll(remapper.getExclusions());
        }

        for (ModRemapper remapper : ModRemappingAPI.MOD_REMAPPERS) {
            for (String jarFolder : remapper.getJarFolders()) {
                File mcSubFolder = new File(FabricLoader.getInstance().getGameDir().toFile(), jarFolder);
                File cacheFolder = new File(Constants.VERSIONED_FOLDER, jarFolder);

                if (!mcSubFolder.exists()) mcSubFolder.mkdirs();
                if (!cacheFolder.exists()) cacheFolder.mkdirs();

                try {
                    Files.walkFileTree(cacheFolder.toPath(), new FileVisitor<Path>() {
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
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                mods.addAll(discoverModsInFolder(mcSubFolder, cacheFolder));
            }
        }

        RemapUtil.generateModMappings();

        Map<Path, Path> modPaths = new HashMap<>();

        for (ModEntry entry : mods) {
            if (entry.original != null) modPaths.put(entry.original.toPath(), entry.file.toPath());

            FakeModManager.addModEntry(entry);
        }

        RemapUtil.remapMods(modPaths);

        for (Path modPath : modPaths.values()) {
            FabricLauncherBase.getLauncher().addToClassPath(modPath);
        }
    }

    private static List<ModEntry> discoverModsInFolder(File folder, File destination) {
        List<ModEntry> mods = new ArrayList<>();

        File[] folderFiles = folder.listFiles();
        if (!folder.isDirectory() || folderFiles == null) return ImmutableList.of();

        for (File file : folderFiles) {
            String name = file.getName();
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
                                FileUtils.excludeFromZipFile(file, EXCLUDED.get(modName.get(0).modName));
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (!modName.isEmpty()) {
                    RemapUtil.makeModMappings(file.toPath());

                    while (!modName.isEmpty()) {
                        ModEntry modname = modName.remove(0);
                        mods.add(modname);
                    }
                }
            }
        }

        return mods;
    }
}
