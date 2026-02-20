package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.visitor;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.ModRemappingAPIImpl;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrClass;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ApiStatus.Internal
public class MixinPostApplyVisitorProvider implements TinyRemapper.ApplyVisitorProvider {
    public MixinPostApplyVisitorProvider() {}

    @Override
    public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next) {
        var node = new ClassNode();
        cls.accept(node, ClassReader.SKIP_FRAMES);

        var className = cls.getName().replace(".", "/");

        var supers = new ArrayList<String>();

        for (List<AnnotationNode> nodeList : new List[]{
                node.visibleAnnotations, node.invisibleAnnotations
        }) {
            if (nodeList == null) continue;
            nodeList.forEach(an -> {
                if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(an.desc)) {
                    int len = an.values.size();

                    for (int i = 0; i < len; i += 2) {
                        Object value = an.values.get(i + 1);

                        if (value instanceof List<?> valueList) {
                            for (Object val : valueList) {
                                String theVal;

                                if (val instanceof Type type) {
                                    theVal = type.getInternalName();
                                } else {
                                    theVal = ModRemappingAPIImpl.getCurrentContext().getMixinData().getMixinRefmapData()
                                            .getOrDefault(className, new HashMap<>()).getOrDefault((String) val, (String) val);
                                }

                                supers.add(theVal);
                            }
                        }
                    }
                }
            });
        }

        ModRemappingAPIImpl.getCurrentContext().getMixinData().getMixin2TargetMap().put(className, supers);

        return next;
    }
}
