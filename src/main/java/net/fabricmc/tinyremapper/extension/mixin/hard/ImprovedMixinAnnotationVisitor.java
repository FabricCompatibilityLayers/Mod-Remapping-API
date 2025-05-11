package net.fabricmc.tinyremapper.extension.mixin.hard;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.ModRemappingAPIImpl;
import net.fabricmc.tinyremapper.extension.mixin.common.data.AnnotationElement;
import net.fabricmc.tinyremapper.extension.mixin.common.data.Constant;
import net.fabricmc.tinyremapper.extension.mixin.common.data.MxClass;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;

public class ImprovedMixinAnnotationVisitor extends AnnotationVisitor {
    private final List<String> targets;
    private final MxClass _class;

    public ImprovedMixinAnnotationVisitor(AnnotationVisitor delegate, List<String> targetsOut, MxClass _class) {
        super(Constant.ASM_VERSION, delegate);

        this.targets = Objects.requireNonNull(targetsOut);
        this._class = Objects.requireNonNull(_class);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        var visitor = super.visitArray(name);

        return switch (name) {
            case AnnotationElement.TARGETS -> new AnnotationVisitor(Constant.ASM_VERSION, visitor) {
                @Override
                public void visit(String name, Object value) {
                    var srcName = ((String) value).replaceAll("\\s", "").replace('.', '/');
                    var dstName = srcName;

                    Map<String, String> refmapData = ModRemappingAPIImpl.getCurrentContext().getMixinData()
                            .getMixinRefmapData()
                            .getOrDefault(ImprovedMixinAnnotationVisitor.this._class.getName(), new HashMap<>());
                    
                    srcName = refmapData.getOrDefault(value, srcName);

                    ImprovedMixinAnnotationVisitor.this.targets.add(srcName);

                    super.visit(name, dstName);
                }
            };
            case AnnotationElement.VALUE -> new AnnotationVisitor(Constant.ASM_VERSION, visitor) {
                @Override
                public void visit(String name, Object value) {
                    if (value instanceof Type srcType) {
                        ImprovedMixinAnnotationVisitor.this.targets.add(srcType.getInternalName());
                    }
                    super.visit(name, value);
                }
            };
            default -> visitor;
        };
    }
}