package fr.catcore.modremapperapi.impl;

import fr.catcore.modremapperapi.api.v1.*;
import net.legacyfabric.fabric.api.logger.v1.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static fr.catcore.modremapperapi.impl.FileUtils.getJarFS;
import static fr.catcore.modremapperapi.impl.FileUtils.openZip;

public class ModExplorer {
    private static final Logger LOGGER = Logger.get("ModRemappingAPI", "ModExplorer");
    public static void start() {
        for (ModRemapper remapper : MRAPIImpl.REMAPPERS) {
            List<ModCandidate> entries = new ArrayList<>();

            for (ModDiscoverer discoverer : remapper.getModDiscoverers()) {
                explore(discoverer, entries);
            }

            remapper.filterModEntries(entries);
        }
    }

    public static void explore(ModDiscoverer discoverer, List<ModCandidate> entries) {
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

                exploreFolder(discoverer, originalPath, cachePath, entries);
            } catch (IOException e) {
                LOGGER.error("{}", e);
            }
        }
    }

    public static void exploreFolder(ModDiscoverer discoverer, Path in, Path out, List<ModCandidate> entries) throws IOException {
        try (Stream<Path> stream = Files.list(in)) {
            stream.forEach(item -> {
                if (Files.isDirectory(item) && discoverer.acceptDirectories()) {
                    // TODO
                } else {
                    try {
                        if (isInteresting(discoverer, item) && !isFabricMod(item)) handleFile(discoverer, item, entries, out);
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

    public static void handleFile(ModDiscoverer discoverer, Path item, List<ModCandidate> entries, Path out) throws IOException, URISyntaxException {
        List<String> zipEntries = new ArrayList<>();
        List<String> mods = new ArrayList<>();

        firstRoundZip(item, discoverer, zipEntries, mods);
        secondRoundZip(item, discoverer, zipEntries, mods);

        if (mods.isEmpty() && discoverer.acceptAnyFile()) {
            try {
                entries.add(new ModCandidateImpl(
                        item,
                        null,
                        discoverer.parseAnyInfos(item),
                        discoverer,
                        out));
            } catch (IOException e) {
                LOGGER.error("{}", e);
            }
        }

        parseModInfosZip(item, discoverer, mods, entries, out);
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

    public static void firstRoundZip(Path mod, ModDiscoverer discoverer, List<String> entries, List<String> mods) throws IOException {
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

    public static void secondRoundZip(Path mod, ModDiscoverer discoverer, List<String> entries, List<String> mods) throws URISyntaxException, IOException {
        try (FileSystem fs = getJarFS(mod)) {
            mods.removeIf(modEntry -> {
                Path modPath = fs.getPath(modEntry);

                try {
                    return !discoverer.isMod(modEntry, modPath);
                } catch (IOException e) {
                    LOGGER.error("{}", e);
                    return true;
                }
            });

            for (String entry : entries) {
                Path entryPath = fs.getPath(entry);

                if (discoverer.isMod(entry, entryPath)) mods.add(entry);
            }
        }
    }

    public static void parseModInfosZip(Path mod, ModDiscoverer discoverer, List<String> mods, List<ModCandidate> entries, Path out) throws URISyntaxException, IOException {
        try (FileSystem fs = getJarFS(mod)) {
            mods.forEach(entry -> {
                Path entryPath = fs.getPath(entry);

                try {
                    ModInfos infos = discoverer.parseModInfos(entry, entryPath);

                    if (infos != null) entries.add(new ModCandidateImpl(
                            mod,
                            entry,
                            infos,
                            discoverer,
                            out));
                } catch (IOException e) {
                    LOGGER.error("{}", e);
                }
            });
        }
    }
}
