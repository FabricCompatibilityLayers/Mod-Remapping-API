package io.github.fabriccompatibilitylayers.modremappingapi.impl.compatibility.v1;

import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.ModRemapper;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.RemapLibrary;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.VisitorInfos;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public class V1DefaultModRemapper implements ModRemapper {
    @Override
    public String[] getJarFolders() {
        return new String[]{"mods"};
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
    public boolean remapMixins() {
        return false;
    }
}
