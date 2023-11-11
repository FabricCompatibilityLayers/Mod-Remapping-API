package fr.catcore.modremapperapi.remapping;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

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

        for (Map.Entry<VisitorInfos.Type, VisitorInfos.Type> entry : infos.METHOD_TYPE.entrySet()) {
            if (entry.getKey().type.equals(type)) {
                superType = entry.getValue();
                break;
            }
        }

        super.visitTypeInsn(opcode, superType.type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        VisitorInfos.MethodNamed superType = new VisitorInfos.MethodNamed(owner, name);

        for (Map.Entry<VisitorInfos.MethodNamed, VisitorInfos.MethodNamed> entry : infos.METHOD_FIELD.entrySet()) {
            if (entry.getKey().owner.equals(owner)) {
                if (entry.getKey().name.isEmpty() || entry.getKey().name.equals(name)) {
                    superType = entry.getValue();
                }
            }
        }

        super.visitFieldInsn(opcode, superType.owner, superType.name.isEmpty() ? name : superType.name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        VisitorInfos.MethodNamed superType = new VisitorInfos.MethodNamed(owner, name);

        for (Map.Entry<VisitorInfos.MethodNamed, VisitorInfos.MethodNamed> entry : infos.METHOD_METHOD.entrySet()) {
            if (entry.getKey().owner.equals(owner)) {
                if (entry.getKey().name.isEmpty() || entry.getKey().name.equals(name)) {
                    superType = entry.getValue();
                }
            }
        }

        super.visitMethodInsn(opcode, superType.owner, superType.name.isEmpty() ? name : superType.name, descriptor, isInterface);
    }

    @Override
    public void visitLdcInsn(Object value) {
        VisitorInfos.MethodValue val = new VisitorInfos.MethodValue(this.className, value);

        for (Map.Entry<VisitorInfos.MethodValue, VisitorInfos.MethodValue> entry : infos.METHOD_LDC.entrySet()) {
            if (entry.getKey().value.equals(value)) {
                val = entry.getValue();
                break;
            }
        }

        super.visitLdcInsn(val.value);
    }
}
