package io.github.fabriccompatibiltylayers.modremappingapi.api.v1;

@Deprecated
public interface MappingBuilder {

    ClassMapping addMapping(String sourceName, String targetName);
    ClassMapping addMapping(String name);

    @Deprecated
    public interface ClassMapping {
        ClassMapping field(String sourceName, String targetName, String sourceDescriptor);
        ClassMapping field(String name, String descriptor);
        ClassMapping method(String sourceName, String targetName, String sourceDescriptor);
        ClassMapping method(String name, String descriptor);
    }
}
