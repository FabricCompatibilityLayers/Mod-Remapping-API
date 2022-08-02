package fr.catcore.modremapperapi;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import static fr.catcore.modremapperapi.ModRemappingAPI.init;

public class ModRemappingApiInit implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        init(false);
    }
}
