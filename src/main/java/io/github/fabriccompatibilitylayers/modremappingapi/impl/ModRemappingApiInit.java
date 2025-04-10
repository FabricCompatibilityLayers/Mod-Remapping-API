package io.github.fabriccompatibilitylayers.modremappingapi.impl;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ModRemappingApiInit implements Runnable {
    public ModRemappingApiInit() {}
    @Override
    public void run() {
        ModRemappingAPIImpl.init();
    }
}
