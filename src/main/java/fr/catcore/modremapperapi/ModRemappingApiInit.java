package fr.catcore.modremapperapi;

import static fr.catcore.modremapperapi.ModRemappingAPI.init;

public class ModRemappingApiInit implements Runnable {
    @Override
    public void run() {
        init(false);
    }
}
