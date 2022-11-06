package fr.catcore.modremapperapi.remapping;

import net.legacyfabric.fabric.api.logger.v1.Logger;
import org.objectweb.asm.*;

public class MRAClassVisitor extends ClassVisitor {
    private final VisitorInfos infos;
    private final String className;
    protected MRAClassVisitor(ClassVisitor classVisitor, VisitorInfos infos, String className) {
        super(Opcodes.ASM9, classVisitor);
        this.infos = infos;
        this.className = className;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        VisitorInfos.Type superType = new VisitorInfos.Type(superName);

        superType = infos.SUPERS.getOrDefault(superType, superType);

        super.visit(version, access, name, signature, superType.type, interfaces);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        VisitorInfos.Type superType = new VisitorInfos.Type(descriptor);

        superType = infos.ANNOTATION.getOrDefault(superType, superType);

        return super.visitTypeAnnotation(typeRef, typePath, superType.type, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
        original = new MRAMethodVisitor(original, infos, this.className);
        return original;
    }
}
