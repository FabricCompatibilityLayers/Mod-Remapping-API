package net.fabricmc.tinyremapper.extension.mixin.hard;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.ModRemappingAPIImpl;
import net.fabricmc.tinyremapper.extension.mixin.common.data.AnnotationElement;
import net.fabricmc.tinyremapper.extension.mixin.common.data.Constant;
import net.fabricmc.tinyremapper.extension.mixin.common.data.MxClass;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
        AnnotationVisitor visitor = super.visitArray(name);

        if (name.equals(AnnotationElement.TARGETS)) {
            return new AnnotationVisitor(Constant.ASM_VERSION, visitor) {
                @Override
                public void visit(String name, Object value) {
                    String srcName = ((String) value).replaceAll("\\s", "").replace('.', '/');
                    String dstName = srcName;

                    srcName = ModRemappingAPIImpl.getCurrentContext().getMixinData()
                            .getMixinRefmapData()
                            .getOrDefault(ImprovedMixinAnnotationVisitor.this._class.getName(), new HashMap<>())
                            .getOrDefault(value, srcName);

                    ImprovedMixinAnnotationVisitor.this.targets.add(srcName);

                    value = dstName;
                    super.visit(name, value);
                }
            };
        } else if (name.equals(AnnotationElement.VALUE)) {
            return new AnnotationVisitor(Constant.ASM_VERSION, visitor) {
                @Override
                public void visit(String name, Object value) {
                    Type srcType = Objects.requireNonNull((Type) value);

                    ImprovedMixinAnnotationVisitor.this.targets.add(srcType.getInternalName());

                    super.visit(name, value);
                }
            };
        } else {
            return visitor;
        }
    }
}