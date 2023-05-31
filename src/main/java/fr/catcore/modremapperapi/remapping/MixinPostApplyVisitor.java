package fr.catcore.modremapperapi.remapping;

import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.TinyTree;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

public class MixinPostApplyVisitor implements TinyRemapper.ApplyVisitorProvider{
    private final List<ClassDef> classDefs = new ArrayList<>();
    public MixinPostApplyVisitor(TinyTree[] trees) {
        for (TinyTree tree : trees) {
            classDefs.addAll(tree.getClasses());
        }
    }

    @Override
    public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next) {
        ClassNode node = new ClassNode();
        cls.accept(node, ClassReader.SKIP_FRAMES);

        List<String> supers = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        List<String> methods = new ArrayList<>();

        for (List<AnnotationNode> nodeList : new List[]{
                node.visibleAnnotations, node.invisibleAnnotations
        }) {
            if (nodeList == null) continue;
            nodeList.forEach(an -> {
                if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(an.desc)) {
                    int len = an.values.size();

                    for (int i = 0; i < len; i += 2) {
                        Object value = an.values.get(i + 1);

                        if (value instanceof List) {
                            for (Object val : (List) value) {
                                if (val instanceof Type) {
                                    supers.add(((Type) val).getInternalName());
                                } else {
                                    supers.add((String) val);
                                }
                            }
                        } else {
                            System.out.println(an.values.get(i) + " : " + value.toString());
                        }
                    }
                }
            });
        }

        if (!supers.isEmpty()) {
            node.fields.forEach(fl -> {
                for (List<AnnotationNode> list : new List[]{
                        fl.visibleAnnotations, fl.invisibleAnnotations
                }) {
                    if (list == null) continue;
                    list.forEach(nd -> {
                        if ("Lorg/spongepowered/asm/mixin/Shadow;".equals(nd.desc)) {
                            fields.add(fl.name);
                        }
                    });
                }
            });
            node.methods.forEach(fl -> {
                for (List<AnnotationNode> list : new List[]{
                        fl.visibleAnnotations, fl.invisibleAnnotations
                }) {
                    if (list == null) continue;
                    list.forEach(nd -> {
                        if ("Lorg/spongepowered/asm/mixin/Shadow;".equals(nd.desc)
                                || "Lorg/spongepowered/asm/mixin/Overwrite;".equals(nd.desc)) {
                            methods.add(fl.name);
                        }
                    });
                }
            });
        } else {
            return next;
        }

        return new MixinExtraVisitor(next, classDefs, supers, fields, methods);
    }
}
