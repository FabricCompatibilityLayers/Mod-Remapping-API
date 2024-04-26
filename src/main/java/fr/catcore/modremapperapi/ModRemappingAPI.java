package fr.catcore.modremapperapi;

import fr.catcore.modremapperapi.api.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModRemappingAPIImpl;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModRemappingAPI {

    public static final boolean BABRIC = FabricLoader.getInstance().getModContainer("fabricloader")
            .get().getMetadata().getVersion().getFriendlyString().contains("babric");
}
