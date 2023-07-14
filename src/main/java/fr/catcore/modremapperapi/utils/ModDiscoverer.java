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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

        File mainTempDir = new File(Constants.VERSIONED_FOLDER, "temp");
        if (mainTempDir.exists()) {
            try {
                Files.walkFileTree(mainTempDir.toPath(), new FileVisitor<Path>() {
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
        }

        RemapUtil.generateModMappings();

        Map<Path, Path> modPaths = new HashMap<>();

        for (ModEntry entry : mods) {
            if (entry.original != null) modPaths.put(entry.original.toPath(), entry.file.toPath());

            FakeModManager.addModEntry(entry);
        }

        if (!ModRemappingAPI.remapClassEdits) {
            modPaths = excludeClassEdits(modPaths);
        }

        RemapUtil.remapMods(modPaths);

        for (Path modPath : modPaths.values()) {
            Constants.MAIN_LOGGER.debug("Adding path to classpath: " + modPath.toString());
            FabricLauncherBase.getLauncher().addToClassPath(modPath);
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut, boolean root) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (!root) {
                if (fileName.endsWith("/")) {
                    zipOut.putNextEntry(new ZipEntry(fileName));
                    zipOut.closeEntry();
                } else {
                    zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                    zipOut.closeEntry();
                }
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, (root ? "" : fileName + "/") + childFile.getName(), zipOut, false);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        zipOut.closeEntry();
    }

    private static Map<Path, Path> excludeClassEdits(Map<Path, Path> modPaths) {
        Map<Path, Path> map = new HashMap<>();
        Map<Path, Path> convertMap = new HashMap<>();

        File mainTempDir = new File(Constants.VERSIONED_FOLDER, "temp");
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
                if (entry.getKey().toFile().isDirectory()) {
                    FileOutputStream fos = new FileOutputStream(entry.getValue().toFile());
                    ZipOutputStream zipOut = new ZipOutputStream(fos);

                    File fileToZip = entry.getKey().toFile();
                    zipFile(fileToZip, fileToZip.getName(), zipOut, true);
                    zipOut.close();
                } else {
                    Files.copy(entry.getKey(), entry.getValue(), StandardCopyOption.REPLACE_EXISTING);
                }

                /* Define ZIP File System Properies in HashMap */
                Map<String, String> zip_properties = new HashMap<>();
                /* We want to read an existing ZIP File, so we set this to False */
                zip_properties.put("create", "false");
                /* Specify the encoding as UTF -8 */
                zip_properties.put("encoding", "UTF-8");

                try (FileSystem zipfs = FileSystems.newFileSystem(new URI("jar:file:" + entry.getValue().toFile().getAbsolutePath()), zip_properties)) {
                    for (String clName : RemapUtil.MC_CLASS_NAMES) {
                        Path classPath = zipfs.getPath("/" + clName + ".class");

                        if (Files.exists(classPath)) {
                            Files.delete(classPath);
                        }
                    }
                }
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                errored.add(entry.getValue());
            }
        }

        for (Path path : errored) {
            map.remove(path);
        }

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
