package fr.catcore.modremapperapi.remapping;

import org.objectweb.asm.*;

import java.util.Map;

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

        for (Map.Entry<VisitorInfos.Type, VisitorInfos.Type> entry : infos.SUPERS.entrySet()) {
            if (entry.getKey().type.equals(superName)) {
                superType = entry.getValue();
                break;
            }
        }

        super.visit(version, access, name, signature, superType.type, interfaces);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        VisitorInfos.Type superType = new VisitorInfos.Type(descriptor);

        for (Map.Entry<VisitorInfos.Type, VisitorInfos.Type> entry : infos.ANNOTATION.entrySet()) {
            if (entry.getKey().type.equals(descriptor)) {
                superType = entry.getValue();
                break;
            }
        }

        return super.visitTypeAnnotation(typeRef, typePath, superType.type, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
        original = new MRAMethodVisitor(original, infos, this.className);
        return original;
    }
}
