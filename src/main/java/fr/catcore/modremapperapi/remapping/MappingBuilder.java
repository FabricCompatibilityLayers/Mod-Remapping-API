package fr.catcore.modremapperapi.remapping;

import fr.catcore.modremapperapi.ModRemappingAPI;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MappingBuilder {
    private static final boolean BABRIC = WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC || WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC_NEW_FORMAT;

    private final String obfucated;
    private final String intermediary;
    private final List<Entry> entries = new ArrayList<>();

    private MappingBuilder(String obfucated, String intermediary) {
        this.obfucated = obfucated;
        this.intermediary = intermediary;
    }

    private static String toString(String... line) {
        StringBuilder builder = new StringBuilder(line[0]);
        for (int j = 1; j < line.length; j++) {
            builder.append('\t');
            builder.append(line[j]);
        }
        return builder.toString();
    }

    public static MappingBuilder create(String obfucated, String intermediary) {
        return new MappingBuilder(obfucated, intermediary);
    }

    public static MappingBuilder create(String name) {
        return new MappingBuilder(name, name);
    }

    public MappingBuilder field(String obfuscated, String intermediary, String description) {
        this.entries.add(new Entry(obfuscated, intermediary, description, Type.FIELD));
        return this;
    }

    public MappingBuilder field(String name, String description) {
        this.entries.add(new Entry(name, name, description, Type.FIELD));
        return this;
    }

    public MappingBuilder method(String obfuscated, String intermediary, String description) {
        this.entries.add(new Entry(obfuscated, intermediary, description, Type.METHOD));
        return this;
    }

    public MappingBuilder method(String name, String description) {
        this.entries.add(new Entry(name, name, description, Type.METHOD));
        return this;
    }

    public List<String> build() {
        List<String> list = new ArrayList<>();
        if (ModRemappingAPI.BABRIC) {
            list.add(toString("CLASS", this.intermediary, this.intermediary, this.obfucated, this.obfucated, this.intermediary));
        } else {
            list.add(toString("CLASS", this.obfucated, this.intermediary, this.intermediary));
        }

        entries.forEach(entry -> list.add(entry.toString(ModRemappingAPI.BABRIC ? this.intermediary : this.obfucated)));

        return list;
    }

    public void accept(MappingVisitor visitor) throws IOException {
        visitor.visitClass(BABRIC ? intermediary : obfucated);
        visitor.visitDstName(MappedElementKind.CLASS, 0, BABRIC ? obfucated : this.intermediary);

        for (Entry entry : this.entries) {
            entry.accept(visitor);
        }
    }

    public static class Entry {
        private final String obfuscated;
        private final String intermediary;
        private final String description;
        private final Type type;

        public Entry(String obfuscated, String intermediary, String description, Type type) {
            this.obfuscated = obfuscated;
            this.intermediary = intermediary;
            this.description = description;
            this.type = type;
        }

        public String toString(String className) {
            if (ModRemappingAPI.BABRIC) {
                return MappingBuilder.toString(this.type.name(), className, this.description, this.intermediary, this.intermediary, this.obfuscated, this.obfuscated, this.intermediary);
            } else {
                return MappingBuilder.toString(this.type.name(), className, this.description, this.obfuscated, this.intermediary, this.intermediary);
            }
        }

        public void accept(MappingVisitor visitor) throws IOException {
            if (type == Type.FIELD) visitor.visitField(BABRIC ? intermediary : obfuscated, description);
            else visitor.visitMethod(BABRIC ? intermediary : obfuscated, description);

            visitor.visitDstName(type == Type.FIELD ? MappedElementKind.FIELD : MappedElementKind.METHOD, 0, BABRIC ? obfuscated : intermediary);
        }
    }

    public enum Type {
        METHOD, FIELD;
    }
}
