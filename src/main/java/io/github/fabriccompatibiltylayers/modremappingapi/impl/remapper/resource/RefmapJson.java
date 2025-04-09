package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModRemappingAPIImpl;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrRemapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RefmapJson {
    public final Map<String, Map<String, String>> mappings = new HashMap<>();
    public final Map<String, Map<String, Map<String, String>>> data = new HashMap<>();

    public void remap(TinyRemapper tiny) {
        this.data.clear();

        // for every class entry we need to remap the mappings of references
        this.mappings.forEach((mixinClass, refmapEntryMap) ->
                refmapEntryMap.replaceAll((originalName, oldMappedName) ->
                        mapRefMapEntry(mixinClass, oldMappedName, tiny)
                )
        );

        this.data.put("named:intermediary", this.mappings);

    }

    public void remap(MappingTree tree, String from, String to) {
        this.data.clear();

        this.mappings.forEach((mixinClass, refmapEntryMap) -> refmapEntryMap.entrySet()
                .stream().filter(this::filterClass)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                .replaceAll((originalName, oldMappedName) ->
                        mapRefMapEntry(mixinClass, oldMappedName, tree, from, to)));
    }

    private boolean filterClass(Map.Entry<String, String> stringStringEntry) {
        String value = stringStringEntry.getValue();

        return value != null &&
                value.contains("/") &&
                !value.contains(";") &&
                !value.contains(":") &&
                !value.contains("(");
    }

    private String mapRefMapEntry(String mixinClass, String old, MappingTree tree, String from, String to) {
        int fromId = tree.getNamespaceId(from);
        int toId = tree.getNamespaceId(to);

        return tree.mapClassName(old, fromId, toId);
    }

    private String mapRefMapEntry(String mixinClass, String old, TinyRemapper remapper) {
        TrRemapper trRemapper = remapper.getEnvironment().getRemapper();
        List<String> supers = ModRemappingAPIImpl.getCurrentContext().getMixinData().getMixin2TargetMap().get(mixinClass);
        // format is:
        // owner + name + quantifier + (desc == null || desc.startsWith("(") ? "" : ":") + desc + (tail != null ? " -> " : "") + tail
        String owner; // can be ""
        String name; // can be ""
        String quantifier; // can be ""
        String desc; // can be ""
        String tail; // can be ""

        // read the entry
        {
            String rest;
            // get tail
            {
                String arrow = " -> ";
                int arrowPosition = old.indexOf(arrow);
                if (arrowPosition == -1) { // tail == null
                    tail = "";
                    rest = old;
                } else {
                    rest = old.substring(0, arrowPosition);
                    tail = old.substring(arrowPosition + arrow.length());
                }
            }

            // get desc
            {
                int separatorPosition = rest.indexOf(":");
                if (separatorPosition == -1) { // separator == null
                    int parenthesisPosition = rest.indexOf("(");
                    if (parenthesisPosition == -1) {
                        desc = "";
                    } else {
                        // if there's no ':', then there must be an opening bracket or **the desc is null**
                        desc = rest.substring(parenthesisPosition);
                        rest = rest.substring(0, parenthesisPosition);
                    }
                } else {
                    desc = rest.substring(separatorPosition + 1);
                    rest = rest.substring(0, separatorPosition);
                }
            }

            // get owner
            {
                if (rest.startsWith("L")) { // owner != null
                    int endPosition = rest.indexOf(";");
                    if (endPosition == -1) {
                        throw new RuntimeException(
                                "Cannot parse refmap entry of class " + mixinClass + ": it starts with 'L', and doesn't contain a ';': " + old);
                    } else {
                        owner = rest.substring(1, endPosition);
                        rest = rest.substring(endPosition + 1); // we don't want the ';' here
                    }
                } else {
                    owner = "";
                }
            }

            // get quantifier
            {
                // try to find either '{', '+' or '*'
                int bracesPosition = rest.indexOf("{");
                if (bracesPosition == -1)
                    bracesPosition = rest.indexOf("*");
                if (bracesPosition == -1)
                    bracesPosition = rest.indexOf("+");

                if (bracesPosition == -1) {
                    // try the * and +
                    quantifier = "";
                } else {
                    quantifier = rest.substring(bracesPosition);
                    rest = rest.substring(0, bracesPosition);
                }
            }

            // get name
            {
                name = rest; // only name is left
//                if (name.isEmpty()) {
//                    throw new RuntimeException("Cannot parse refmap entry of class " + mixinClass +
//                            ": the name is \"\", so something went wrong: owner = \"" + owner + "\", name = \"" + name +
//                            "\", quantifier = \"" + quantifier + "\", desc = \"" + desc + "\", tail = \"" + tail +
//                            "\", old = \"" + old + "\"");
//                }
            }
        }

        // for now just stop here, most stuff doesn't use quantifiers or tails
        if (!quantifier.isEmpty())
            throw new RuntimeException("Quantifiers are not yet supported: " + old);
        if (!tail.isEmpty())
            throw new RuntimeException("Tails are not yet supported: " + tail);

        // do the actual mapping

        // it's a class
        if (owner.isEmpty() && desc.isEmpty()) {
            return trRemapper.map(name);
        }

        // it's a method
        if (desc.startsWith("(") && desc.contains(")")) {
            if (owner.isEmpty()) { // it's an @Invoker
                if (supers == null || supers.isEmpty()) {
                    throw new RuntimeException("Can't find target class for mixin " + mixinClass);
                }

                final String originalName = name;
                String newDesc = trRemapper.mapMethodDesc(desc);

                if (!originalName.isEmpty()) {
                    for (String own : supers) {
                        name = trRemapper.mapMethodName(own, name, desc);

                        if (!originalName.equals(name)) {
                            return name + newDesc;
                        }
                    }
                }

                return originalName + newDesc;
            } else { // just a normal method
                return "L" + trRemapper.map(owner) + ";" + trRemapper.mapMethodName(owner, name, desc) + trRemapper.mapMethodDesc(desc);
            }
        }

        // it's an @Accessor
        if (owner.isEmpty()) {
            if (supers == null || supers.isEmpty()) {
                throw new RuntimeException("Can't find target class for mixin " + mixinClass);
            }

            final String originalName = name;

            for (String own : supers) {
                name = trRemapper.mapFieldName(own, name, desc);

                if (!originalName.equals(name)) {
                    return name;
                }
            }

            return originalName;
        }

        // just a normal field
        return "L" + trRemapper.map(owner) + ";" + trRemapper.mapFieldName(owner, name, desc) + ":" + trRemapper.mapDesc(desc);
    }
}
