package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

public interface MappingBuilder {

    ClassMapping addMapping(String sourceName, String targetName);
    ClassMapping addMapping(String name);

    public interface ClassMapping {
        ClassMapping field(String sourceName, String targetName, String sourceDescriptor);
        ClassMapping field(String name, String descriptor);
        ClassMapping method(String sourceName, String targetName, String sourceDescriptor);
        ClassMapping method(String name, String descriptor);
    }
}
