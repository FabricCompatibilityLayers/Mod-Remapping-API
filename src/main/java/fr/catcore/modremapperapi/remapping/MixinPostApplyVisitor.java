package fr.catcore.modremapperapi.remapping;

import fr.catcore.modremapperapi.utils.Constants;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MixinPostApplyVisitor implements TinyRemapper.ApplyVisitorProvider{
    private final List<MappingTree.ClassMapping> classDefs = new ArrayList<>();
    public MixinPostApplyVisitor(MappingTree[] trees) {
        for (MappingTree tree : trees) {
            classDefs.addAll(tree.getClasses());
        }
    }

    @Override
    public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next) {
        ClassNode node = new ClassNode();
        cls.accept(node, ClassReader.SKIP_FRAMES);

        List<String> supers = new ArrayList<>();
        Map<String, List<String>> fields = new HashMap<>();
        Map<String, List<String>> methods = new HashMap<>();

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
                            Constants.MAIN_LOGGER.info(an.values.get(i) + " : " + value.toString());
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
                            fields.compute(fl.name, (s, strings) -> {
                                if (strings == null) {
                                    strings = new ArrayList<>();
                                }

                                strings.add(fl.desc);

                                return strings;
                            });
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
                            methods.compute(fl.name, (s, strings) -> {
                                if (strings == null) {
                                    strings = new ArrayList<>();
                                }

                                strings.add(fl.desc);

                                return strings;
                            });
                        }
                    });
                }
            });

            StringBuilder str = new StringBuilder("=====================================\nFound Mixin class %s, looking for Shadow annotations.\nDetected super classes:");
            supers.forEach(s -> str.append("\n- ").append(s));
            str.append("\nFields to remap:");
            fields.forEach((s, strings) -> {
                str.append("\n- ").append(s);
                strings.forEach(ss -> str.append("\n  - ").append(ss));
            });
            str.append("\nMethods to remap:");
            methods.forEach((s, strings) -> {
                str.append("\n- ").append(s);
                strings.forEach(ss -> str.append("\n  - ").append(ss));
            });
            str.append("\n=====================================");
            Constants.MAIN_LOGGER.info(str.toString(), node.name);
        } else {
            return next;
        }

        return new MixinExtraVisitor(next, classDefs, supers, fields, methods);
    }
}
