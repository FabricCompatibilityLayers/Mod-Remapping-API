package fr.catcore.modremapperapi.test.impl;

import fr.catcore.modremappingapi.api.v1.ModInfos;

public class MLModInfos implements ModInfos {
    private final String name, id, version;
    protected MLModInfos(String name, String id, String version) {
        this.name = name;
        this.id = id;
        this.version = version;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getType() {
        return "ModLoader";
    }
}
