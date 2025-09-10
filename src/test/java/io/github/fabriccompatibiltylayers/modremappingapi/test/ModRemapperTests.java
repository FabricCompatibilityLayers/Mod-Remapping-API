package io.github.fabriccompatibiltylayers.modremappingapi.test;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.SoftLockFixer;
import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ModRemapperTests {
    @Test
    public void differentSourceNamespace() {
        Assertions.assertEquals(
                MappingUtils.mapClass("net/minecraft/unmapped/C_0760609"),
                FabricLoader.getInstance().isDevelopmentEnvironment() ?
                        "net/minecraft/client/class_785"
                        : "net/minecraft/class_785"
        );
    }

    @Test
    public void testSoftLockFixer() {
        SoftLockFixer.preloadClasses();
    }
}
