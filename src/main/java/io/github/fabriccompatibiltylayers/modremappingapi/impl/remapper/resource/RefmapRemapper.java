package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.resource;

import com.google.gson.Gson;
import fr.catcore.modremapperapi.remapping.RemapUtil;
import fr.catcore.modremapperapi.utils.RefmapJson;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrRemapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RefmapRemapper implements OutputConsumerPath.ResourceRemapper {
    private Gson GSON;
    @Override
    public boolean canTransform(TinyRemapper remapper, Path relativePath) {
        return relativePath.toString().toLowerCase().contains("refmap") && relativePath.toString().endsWith(".json");
    }

    @Override
    public void transform(Path destinationDirectory, Path relativePath, InputStream input, TinyRemapper remapper) throws IOException {
        Path outputFile = destinationDirectory.resolve(relativePath);
        Path outputDir = outputFile.getParent();
        if (outputDir != null) Files.createDirectories(outputDir);

        if (GSON == null) GSON = new Gson();

        RefmapJson json = GSON.fromJson(new InputStreamReader(input), RefmapJson.class);

        json.remap(this, remapper);

        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
            DataOutputStream outputStream = new DataOutputStream(os);
            outputStream.write(GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    public String mapRefMapEntry(String mixinClass, String old, TinyRemapper remapper) {
        TrRemapper trRemapper = remapper.getEnvironment().getRemapper();
        List<String> supers = RemapUtil.MIXINED.get(mixinClass);
        // format is:
        // owner + name + quantifier + (desc == null || desc.startsWith("(") ? "" : ":") + desc + (tail != null ? " -> " : "") + tail
        String owner; // can be ""
        String name;
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
                if (name.isEmpty()) {
                    throw new RuntimeException("Cannot parse refmap entry of class " + mixinClass +
                            ": the name is \"\", so something went wrong: owner = \"" + owner + "\", name = \"" + name +
                            "\", quantifier = \"" + quantifier + "\", desc = \"" + desc + "\", tail = \"" + tail +
                            "\", old = \"" + old + "\"");
                }
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

                for (String own : supers) {
                    name = trRemapper.mapMethodName(own, name, desc);

                    if (!originalName.equals(name)) {
                        return name + newDesc;
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
