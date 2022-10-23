package fr.catcore.modremapperapi.remapping;

import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrClass;
import org.objectweb.asm.*;

public class MRAPostApplyVisitor implements TinyRemapper.ApplyVisitorProvider {
    private VisitorInfos infos;
    @Override
    public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next) {
        final String className = cls.getName();
        return new ClassVisitor(Opcodes.ASM9, next) {
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
                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        VisitorInfos.Type superType = new VisitorInfos.Type(type);

                        superType = infos.METHOD_TYPE.getOrDefault(superType, superType);

                        super.visitTypeInsn(opcode, superType.type);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        VisitorInfos.MethodNamed superType = new VisitorInfos.MethodNamed(owner, name);

                        superType = infos.METHOD_FIELD.getOrDefault(superType, superType);

                        super.visitFieldInsn(opcode, superType.owner, superType.name.isEmpty() ? name : superType.name, descriptor);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        VisitorInfos.MethodNamed superType = new VisitorInfos.MethodNamed(owner, name);

                        superType = infos.METHOD_METHOD.getOrDefault(superType, superType);

                        super.visitMethodInsn(opcode, superType.owner, superType.name.isEmpty() ? name : superType.name, descriptor, isInterface);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        VisitorInfos.MethodValue val = new VisitorInfos.MethodValue(className, value);

                        val = infos.METHOD_LDC.getOrDefault(val, val);

                        super.visitLdcInsn(val.value);
                    }
                };
            }
        };
    }

    public void setInfos(VisitorInfos infos) {
        this.infos = infos;
    }
}
