package io.github.fabriccompatibilitylayers.modremappingapi.impl.compatibility.v1;

import io.github.fabriccompatibiltylayers.modremappingapi.api.v1.MappingBuilder;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class V1MappingBuilderImpl implements MappingBuilder {
    private final MemoryMappingTree next;

    public V1MappingBuilderImpl(MemoryMappingTree next) {
        this.next = next;
    }

    @Override
    public ClassMapping addMapping(String sourceName, String targetName) {
        this.next.visitClass(sourceName);
        this.next.visitDstName(MappedElementKind.CLASS, 0, targetName);
        return new ClassMappingImpl(sourceName, targetName, next);
    }

    @Override
    public ClassMapping addMapping(String name) {
        this.next.visitClass(name);
        return new ClassMappingImpl(name, null, next);
    }

    private static class ClassMappingImpl implements ClassMapping {
        private final String sourceName;
        private final @Nullable String targetName;
        private final MemoryMappingTree next;

        public ClassMappingImpl(String sourceName, @Nullable String targetName, MemoryMappingTree next) {
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.next = next;
        }

        private void visit() {
            this.next.visitClass(sourceName);
            if (targetName != null) this.next.visitDstName(MappedElementKind.CLASS, 0, targetName);
        }

        @Override
        public ClassMapping field(String sourceName, String targetName, String sourceDescriptor) {
            visit();

            this.next.visitField(sourceName, sourceDescriptor);
            if (targetName != null) this.next.visitDstName(MappedElementKind.FIELD, 0, targetName);

            return this;
        }

        @Override
        public ClassMapping field(String name, String descriptor) {
            return this.field(name, null, descriptor);
        }

        @Override
        public ClassMapping method(String sourceName, String targetName, String sourceDescriptor) {
            visit();

            this.next.visitMethod(sourceName, sourceDescriptor);
            if (targetName != null) this.next.visitDstName(MappedElementKind.METHOD, 0, targetName);

            return this;
        }

        @Override
        public ClassMapping method(String name, String descriptor) {
            return this.method(name, null, descriptor);
        }
    }
}
