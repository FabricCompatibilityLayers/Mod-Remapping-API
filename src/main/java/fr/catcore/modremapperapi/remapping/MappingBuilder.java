package fr.catcore.modremapperapi.remapping;

import fr.catcore.wfvaio.FabricVariants;
import fr.catcore.wfvaio.WhichFabricVariantAmIOn;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class MappingBuilder {
    private static final boolean BABRIC = WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC || WhichFabricVariantAmIOn.getVariant() == FabricVariants.BABRIC_NEW_FORMAT;

    private final String obfucated;
    private final String intermediary;
    private final List<Entry> entries = new ArrayList<>();

    private MappingBuilder(String obfucated, String intermediary) {
        this.obfucated = obfucated;
        this.intermediary = intermediary;
    }

    @ApiStatus.Internal
    public static MappingBuilder create(String obfucated, String intermediary) {
        return new MappingBuilder(obfucated, intermediary);
    }

    @ApiStatus.Internal
    public static MappingBuilder create(String name) {
        return new MappingBuilder(name, name);
    }

    @Deprecated
    public MappingBuilder field(String obfuscated, String intermediary, String description) {
        this.entries.add(new Entry(obfuscated, intermediary, description, Type.FIELD));
        return this;
    }

    @Deprecated
    public MappingBuilder field(String name, String description) {
        this.entries.add(new Entry(name, name, description, Type.FIELD));
        return this;
    }

    @Deprecated
    public MappingBuilder method(String obfuscated, String intermediary, String description) {
        this.entries.add(new Entry(obfuscated, intermediary, description, Type.METHOD));
        return this;
    }

    @Deprecated
    public MappingBuilder method(String name, String description) {
        this.entries.add(new Entry(name, name, description, Type.METHOD));
        return this;
    }

    @ApiStatus.Internal
    public void accept(MappingVisitor visitor) throws IOException {
        visitor.visitClass(BABRIC ? intermediary : obfucated);
        visitor.visitDstName(MappedElementKind.CLASS, 0, BABRIC ? obfucated : this.intermediary);

        for (Entry entry : this.entries) {
            entry.accept(visitor);
        }
    }

    @ApiStatus.Internal
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

        @ApiStatus.Internal
        public void accept(MappingVisitor visitor) throws IOException {
            if (type == Type.FIELD) visitor.visitField(BABRIC ? intermediary : obfuscated, description);
            else visitor.visitMethod(BABRIC ? intermediary : obfuscated, description);

            visitor.visitDstName(type == Type.FIELD ? MappedElementKind.FIELD : MappedElementKind.METHOD, 0, BABRIC ? obfuscated : intermediary);
        }
    }

    @ApiStatus.Internal
    public enum Type {
        METHOD, FIELD;
    }
}
