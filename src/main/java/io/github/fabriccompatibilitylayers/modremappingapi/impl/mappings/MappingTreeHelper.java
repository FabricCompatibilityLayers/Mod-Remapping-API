package io.github.fabriccompatibilitylayers.modremappingapi.impl.mappings;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.ModRemappingAPIImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.*;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiStatus.Internal
public class MappingTreeHelper {

    @ApiStatus.Internal
    public static MemoryMappingTree mergeIntoNew(MappingTree left, MappingTree right) throws IOException {
        MemoryMappingTree result = new MemoryMappingTree();

        if (!Objects.equals(left.getSrcNamespace(), right.getSrcNamespace())) {
            throw new RuntimeException("Source namespace mismatch!");
        }

        result.visitHeader();

        var dstNamespaces = new ArrayList<>(left.getDstNamespaces());

        for (String dstNamespace : right.getDstNamespaces()) {
            if (!dstNamespaces.contains(dstNamespace)) {
                dstNamespaces.add(dstNamespace);
            }
        }

        result.visitNamespaces(left.getSrcNamespace(), dstNamespaces);
        result.visitEnd();

        MemoryMappingTree reorderedLeft = new MemoryMappingTree();
        left.accept(new MappingDstNsReorder(reorderedLeft, dstNamespaces));
        MemoryMappingTree reorderedRight = new MemoryMappingTree();
        right.accept(new MappingDstNsReorder(reorderedRight, dstNamespaces));

        reorderedLeft.accept(result, VisitOrder.createByName());
        reorderedRight.accept(result, VisitOrder.createByName());

        return result;
    }

    @ApiStatus.Internal
    public static void merge(VisitableMappingTree main, MappingTree additional) throws IOException {
        if (!Objects.equals(additional.getSrcNamespace(), main.getSrcNamespace())) {
            MemoryMappingTree reorder = new MemoryMappingTree();

            MappingVisitor visitor = new MappingSourceNsSwitch(reorder, main.getSrcNamespace());

            if (!additional.getDstNamespaces().contains(main.getSrcNamespace())) {
                List<String> dstNamespaces = new ArrayList<>(additional.getDstNamespaces());
                dstNamespaces.add(main.getSrcNamespace());

                visitor = new MappingDstNsReorder(visitor, dstNamespaces);
            }

            additional.accept(visitor);

            additional = reorder;
        }

        MemoryMappingTree reordered = new MemoryMappingTree();

        additional.accept(new MappingDstNsReorder(reordered, main.getDstNamespaces()));

        reordered.accept(main, VisitOrder.createByInputOrder());
    }

    @ApiStatus.Internal
    public static MemoryMappingTree readMappings(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            long time = System.currentTimeMillis();
            MemoryMappingTree mappingTree = new MemoryMappingTree();

            // We will only ever need to read tiny here
            // so to strip the other formats from the included copy of mapping IO, don't use MappingReader.read()
            reader.mark(4096);
            final MappingFormat format = MappingReader.detectFormat(reader);
            reader.reset();

            switch (format) {
                case TINY_FILE -> Tiny1FileReader.read(reader, mappingTree);
                case TINY_2_FILE -> Tiny2FileReader.read(reader, mappingTree);
                default -> throw new UnsupportedOperationException("Unsupported mapping format: " + format);
            }

            Log.debug(LogCategory.MAPPINGS, "Loading mappings took %d ms", System.currentTimeMillis() - time);

            return mappingTree;
        }
    }

    @ApiStatus.Internal
    public static IMappingProvider createMappingProvider(MappingTree mappings, String from, String to) {
        return TinyUtils.createMappingProvider(mappings, from, to);
    }

    @ApiStatus.Internal
    public static MemoryMappingTree createMappingTree() throws IOException {
        MappingsRegistry registry = ModRemappingAPIImpl.getCurrentContext().getMappingsRegistry();

        return createMappingTree(registry.getSourceNamespace(), registry.getTargetNamespace());
    }

    @ApiStatus.Internal
    public static MemoryMappingTree createMappingTree(String src, String target) throws IOException {
        MemoryMappingTree mappingTree = new MemoryMappingTree();

        mappingTree.visitHeader();

        List<String> namespaces = new ArrayList<>();
        namespaces.add(target);

        mappingTree.visitNamespaces(src, namespaces);

        return mappingTree;
    }

    @ApiStatus.Internal
    public static void exportMappings(MappingTreeView mappingTreeView, Path outputPath) throws IOException {
        try (MappingWriter writer = MappingWriter.create(outputPath, MappingFormat.TINY_2_FILE)) {
            mappingTreeView.accept(writer);
        }
    }

    public static @NotNull MappingVisitor getNsReorderingVisitor(MemoryMappingTree tempTree, boolean switchNamespace, Map<String, String> renames) {
        List<String> targetNamespace = new ArrayList<>();
        targetNamespace.add("intermediary");

        if (FabricLoader.getInstance().getMappingResolver().getNamespaces().contains("named")) {
            targetNamespace.add("named");
        }

        MappingVisitor visitor = tempTree;

        if (switchNamespace) {
            visitor = new MappingSourceNsSwitch(
                    new MappingDstNsReorder(
                            visitor,
                            targetNamespace
                    ),
                    "official"
            );
        }

        visitor = new MappingNsRenamer(visitor, renames);
        return visitor;
    }
}
