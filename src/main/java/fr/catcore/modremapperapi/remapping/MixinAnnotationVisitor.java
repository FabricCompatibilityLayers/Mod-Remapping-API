package fr.catcore.modremapperapi.remapping;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class MixinAnnotationVisitor extends AnnotationVisitor {

    private final String descriptor;
    private final MixinExtraVisitor extraVisitor;
    protected MixinAnnotationVisitor(AnnotationVisitor annotationVisitor, String descriptor, MixinExtraVisitor extraVisitor) {
        super(Opcodes.ASM9, annotationVisitor);
        this.descriptor = descriptor;
        this.extraVisitor = extraVisitor;
    }

    @Override
    public void visit(String name, Object value) {
        if (descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
            if (name == null) {
                if (value instanceof Type) {
                    extraVisitor.supers.add(((Type) value).getInternalName());
                } else {
                    extraVisitor.supers.add((String) value);
                }
            }
        }

        super.visit(name, value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        if (descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
            return new MixinAnnotationVisitor(super.visitArray(name), this.descriptor, this.extraVisitor);
        }
        return super.visitArray(name);
    }
}
