package io.github.fabriccompatibiltylayers.modremappingapi.impl;

public class ModRemappingApiInit implements Runnable {
    public ModRemappingApiInit() {}
    @Override
    public void run() {
        ModRemappingAPIImpl.init();
    }
}
