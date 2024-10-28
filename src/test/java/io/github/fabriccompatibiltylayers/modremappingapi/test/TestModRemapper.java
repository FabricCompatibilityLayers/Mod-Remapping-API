package io.github.fabriccompatibiltylayers.modremappingapi.test;

import io.github.fabriccompatibiltylayers.modremappingapi.api.MappingUtils;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.RemapLibrary;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.VisitorInfos;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class TestModRemapper implements ModRemapper {
    private static final ModContainer MOD_CONTAINER = FabricLoader.getInstance().getModContainer("mod-remapping-api-test-mod").get();

    @Override
    public String[] getJarFolders() {
        return new String[]{"test-mods"};
    }

    @Override
    public void addRemapLibraries(List<RemapLibrary> libraries, EnvType environment) {

    }

    @Override
    public void registerMappings(MappingBuilder list) {

    }

    @Override
    public void registerPreVisitors(VisitorInfos infos) {

    }

    @Override
    public void registerPostVisitors(VisitorInfos infos) {

    }

    @Override
    public Optional<String> getSourceNamespace() {
        return Optional.of("calamus");
    }

    @Override
    public Optional<Supplier<InputStream>> getExtraMapping() {
        return Optional.of(() -> {
            try {
                return Files.newInputStream(MOD_CONTAINER.findPath("./calamus-1.6.4.tiny").get());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void afterRemap() {
//        assert Objects.equals(
//                MappingUtils.mapClass("net/minecraft/unmapped/C_0760609"),
//                FabricLoader.getInstance().isDevelopmentEnvironment() ?
//                        "net/minecraft/client/class_785"
//                        : "net/minecraft/class_785"
//        );
    }
}
