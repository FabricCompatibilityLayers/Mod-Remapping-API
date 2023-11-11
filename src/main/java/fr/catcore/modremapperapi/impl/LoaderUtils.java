package fr.catcore.modremapperapi.impl;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class LoaderUtils {
    private static Path getFabricMCPath() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static Path getMCFolder(String name) {
        return getFabricMCPath().resolve(name);
    }

    public static Path getMainFolder() {
        return getMCFolder("mod-remapping-api");
    }

    private static String getMCVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString();
    }

    private static String getLoaderName() {
        return "fabricloader";
    }

    private static String getMRAPIVersion() {
        return FabricLoader.getInstance().getModContainer("mod-remapping-api").get().getMetadata().getVersion().getFriendlyString();
    }

    public static Path getVersionedFolder() {
        return getMainFolder().resolve(getLoaderName()).resolve(getMCVersion()).resolve(getMRAPIVersion());
    }
}
