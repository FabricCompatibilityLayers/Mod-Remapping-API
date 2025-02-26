package io.github.fabriccompatibiltylayers.modremappingapi.impl.mappings;

import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitOrder;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MappingTreeHelper {
    public static void mergeIntoNew(VisitableMappingTree result, MappingTree left, MappingTree right) throws IOException {
        assert Objects.equals(left.getSrcNamespace(), right.getSrcNamespace());

        result.visitHeader();

        List<String> dstNamespaces = new ArrayList<>(left.getDstNamespaces());

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
    }

    public static void merge(VisitableMappingTree main, MappingTree additional) throws IOException {
        if (!Objects.equals(additional.getSrcNamespace(), main.getSrcNamespace())) {
            MemoryMappingTree reorder = new MemoryMappingTree();

            MappingVisitor visitor = new MappingSourceNsSwitch(reorder, main.getSrcNamespace());

            if (!additional.getDstNamespaces().contains(main.getSrcNamespace())) {
                List<String> dstNamespaces = new ArrayList<>(additional.getDstNamespaces());
                dstNamespaces.add(main.getSrcNamespace());

                visitor = new MappingDstNsReorder(reorder, dstNamespaces);
            }

            additional.accept(visitor);

            additional = reorder;
        }

        MemoryMappingTree reordered = new MemoryMappingTree();

        additional.accept(new MappingDstNsReorder(reordered, main.getDstNamespaces()));

        reordered.accept(main, VisitOrder.createByInputOrder());
    }
}
