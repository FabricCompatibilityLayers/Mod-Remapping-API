package fr.catcore.modremapperapi.remapping;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MRAMethodVisitor extends MethodVisitor {
    private final VisitorInfos infos;
    private final String className;
    protected MRAMethodVisitor(MethodVisitor methodVisitor, VisitorInfos visitorInfos, String className) {
        super(Opcodes.ASM9, methodVisitor);
        this.infos = visitorInfos;
        this.className = className;
    }

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
        VisitorInfos.MethodValue val = new VisitorInfos.MethodValue(this.className, value);

        val = infos.METHOD_LDC.getOrDefault(val, val);

        super.visitLdcInsn(val.value);
    }
}
