package fr.catcore.modremapperapi.remapping;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class MixinExtraVisitor extends ClassVisitor {
    protected final List<String> supers = new ArrayList<>();
    protected MixinExtraVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return new MixinAnnotationVisitor(super.visitAnnotation(descriptor, visible), descriptor, this);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }
}
