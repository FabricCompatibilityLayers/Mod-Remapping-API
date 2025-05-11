package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.asm;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.VisitorInfosImpl;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.*;

@ApiStatus.Internal
public class MRAClassVisitor extends ClassVisitor {
    private final VisitorInfosImpl infos;
    private final String className;

    public MRAClassVisitor(ClassVisitor classVisitor, VisitorInfosImpl infos, String className) {
        super(Opcodes.ASM9, classVisitor);
        this.infos = infos;
        this.className = className;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        var superType = infos.SUPERS.getOrDefault(superName, superName);
        super.visit(version, access, name, signature, superType, interfaces);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        var annotationType = infos.ANNOTATION.getOrDefault(descriptor, descriptor);
        return super.visitTypeAnnotation(typeRef, typePath, annotationType, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        var original = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MRAMethodVisitor(original, infos, this.className);
    }
}
