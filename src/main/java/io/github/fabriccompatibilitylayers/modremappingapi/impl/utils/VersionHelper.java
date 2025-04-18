package io.github.fabriccompatibilitylayers.modremappingapi.impl.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class VersionHelper {
    private static final Version MC_VERSION = FabricLoader.getInstance().getModContainer("minecraft")
            .get().getMetadata().getVersion();

    public static final String MINECRAFT_VERSION = MC_VERSION.getFriendlyString();
    public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer("mod-remapping-api").get().getMetadata().getVersion().getFriendlyString();

    public static Boolean predicate(String predicate) {
        try {
            return VersionPredicate.parse(predicate).test(MC_VERSION);
        } catch (VersionParsingException e) {
            return null;
        }
    }
}
