package fr.catcore.modremapperapi.impl;

import fr.catcore.modremapperapi.api.v1.Constants;
import fr.catcore.modremapperapi.api.v1.ModDiscoverer;
import fr.catcore.modremapperapi.api.v1.ModRemapper;
import net.legacyfabric.fabric.api.logger.v1.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static fr.catcore.modremapperapi.impl.FileUtils.getJarFS;
import static fr.catcore.modremapperapi.impl.FileUtils.openZip;

public class ModExplorer {
    private static final Logger LOGGER = Logger.get("ModRemappingAPI", "ModExplorer");
    public static void start() {
        for (ModRemapper remapper : MRAPIImpl.REMAPPERS) {
            for (ModDiscoverer discoverer : remapper.getModDiscoverers()) {
                explore(discoverer);
            }
        }
    }

    public static void explore(ModDiscoverer discoverer) {
        for (String folderName : discoverer.getModFolders()) {
            Path originalPath = LoaderUtils.getMCFolder(folderName);
            Path cachePath = Constants.CACHE_FOLDER.resolve(folderName);

            try {
                if (!Files.exists(originalPath)) {
                    Files.createDirectories(originalPath);
                    continue;
                } else if (!Files.isDirectory(originalPath)) {
                    LOGGER.warn("Path {} is not a directory, skipping!", originalPath);
                    continue;
                }

                if (!Files.exists(cachePath)) Files.createDirectories(cachePath);
                else FileUtils.emptyFolder(cachePath);

                exploreFolder(discoverer, originalPath, cachePath);
            } catch (IOException e) {
                // TODO
            }
        }
    }

    public static void exploreFolder(ModDiscoverer discoverer, Path in, Path out) throws IOException {
        try (Stream<Path> stream = Files.list(in)) {
            stream.forEach(item -> {
                if (Files.isDirectory(item) && discoverer.acceptDirectories()) {
                    // TODO
                } else {
                    try {
                        if (isInteresting(discoverer, item) && !isFabricMod(item)) handleFile(discoverer, item);
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    public static boolean isInteresting(ModDiscoverer discoverer, Path item) {
        String fileName = item.getFileName().toString();

        boolean open = false;

        for (String ext : discoverer.getModExtensions()) {
            if (fileName.endsWith(ext)) {
                open = true;
                break;
            }
        }

        return open;
    }

    public static void handleFile(ModDiscoverer discoverer, Path item) throws IOException, URISyntaxException {
        if (discoverer.acceptAnyFile()) {
            // TODO
        } else {
            List<String> zipEntries = new ArrayList<>();
            List<String> mods = new ArrayList<>();

            firstRound(item, discoverer, zipEntries, mods);
            secondRound(item, discoverer, zipEntries, mods);
        }
    }

    public static boolean isFabricMod(Path item) throws IOException {
        AtomicBoolean fabric = new AtomicBoolean(false);

        openZip(item, (entry, fullName, shortName) -> {
            if (!entry.isDirectory()) {
                if (shortName.equals("fabric.mod.json") || shortName.equals("quilt.mod.json")) {
                    fabric.set(true);
                    return true;
                }
            }

            return false;
        });

        return fabric.get();
    }

    public static void firstRound(Path mod, ModDiscoverer discoverer, List<String> entries, List<String> mods) throws IOException {
        openZip(mod, (entry, fullName, shortName) -> {
            if (!entry.isDirectory()) {
                entries.add(fullName);

                if (discoverer.isMod(fullName)) {
                    mods.add(fullName);
                }
            }

            return false;
        });
    }

    public static void secondRound(Path mod, ModDiscoverer discoverer, List<String> entries, List<String> mods) throws URISyntaxException, IOException {
        try (FileSystem fs = getJarFS(mod)) {
            mods.removeIf(modEntry -> {
                Path modPath = fs.getPath(modEntry);

                return !discoverer.isMod(modEntry, modPath);
            });

            for (String entry : entries) {
                Path entryPath = fs.getPath(entry);

                if (discoverer.isMod(entry, entryPath)) mods.add(entry);
            }
        }
    }


}
