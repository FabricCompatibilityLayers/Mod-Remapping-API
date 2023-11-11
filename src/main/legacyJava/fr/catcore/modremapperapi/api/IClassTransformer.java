package fr.catcore.modremapperapi.api;

// Original author is gudenau.
public interface IClassTransformer {
    boolean handlesClass(String name, String transformedName);
    byte[] transformClass(String name, String transformedName, byte[] original);
}
