package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.asm;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.VisitorInfos;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.VisitorInfosImpl;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

public class MRAMethodVisitor extends MethodVisitor implements Opcodes {
    private final VisitorInfosImpl infos;
    private final String className;
    protected MRAMethodVisitor(MethodVisitor methodVisitor, VisitorInfosImpl visitorInfos, String className) {
        super(Opcodes.ASM9, methodVisitor);
        this.infos = visitorInfos;
        this.className = className;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        String currentType = type;

        boolean skip = false;

        if (opcode == NEW && infos.INSTANTIATION.containsKey(type)) {
            currentType = infos.INSTANTIATION.get(type);
            skip = true;
        }

        if (!skip && infos.METHOD_TYPE.containsKey(type)) {
            currentType = infos.METHOD_TYPE.get(type);
        }

        super.visitTypeInsn(opcode, currentType);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        String currentOwner = owner;
        String currentName = name;
        String currentDescriptor = descriptor;

        if (infos.FIELD_REF.containsKey(owner)) {
            Map<String, Map<String, VisitorInfos.FullClassMember>> fields = infos.FIELD_REF.get(owner);

            Map<String, VisitorInfos.FullClassMember> args = fields.get(name);

            if (args == null) {
                args = fields.get("");
            }

            if (args != null) {
                VisitorInfos.FullClassMember classMember = args.get(descriptor);

                if (classMember == null) {
                    classMember = args.get("");
                }

                if (classMember != null) {
                    currentOwner = classMember.getOwner();
                    currentName = classMember.getName();
                    currentDescriptor  = classMember.getDesc();
                }
            }
        }

        if (currentName.isEmpty()) {
            currentName = name;
        }

        if (currentDescriptor == null || currentDescriptor.isEmpty()) {
            currentDescriptor = descriptor;
        }

        super.visitFieldInsn(opcode, currentOwner, currentName, currentDescriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        int currentOpcode = opcode;
        String currentOwner = owner;
        String currentName = name;
        String currentDescriptor = descriptor;

        boolean skip = false;

        if (opcode == INVOKESPECIAL && infos.INSTANTIATION.containsKey(owner) && name.equals("<init>")) {
            currentOwner = infos.INSTANTIATION.get(owner);
            skip = true;
        }

        if (!skip && (opcode == INVOKEVIRTUAL || opcode == INVOKESTATIC)) {
            if (infos.METHOD_INVOCATION.containsKey(owner)) {
                Map<String, Map<String, VisitorInfos.FullClassMember>> methods = infos.METHOD_INVOCATION.get(owner);

                Map<String, VisitorInfos.FullClassMember> args = methods.get(currentName);

                if (args == null) {
                    args = methods.get("");
                }

                if (args != null) {
                    VisitorInfos.FullClassMember fullClassMember = args.get(currentDescriptor);

                    if (fullClassMember == null) {
                        fullClassMember = args.get("");
                    }

                    if (fullClassMember != null) {
                        currentOwner = fullClassMember.getOwner();
                        currentName = fullClassMember.getName();
                        currentDescriptor  = fullClassMember.getDesc();

                        if (fullClassMember.isStatic() != null) currentOpcode = fullClassMember.isStatic() ? INVOKESTATIC : INVOKEVIRTUAL;
                    }
                }
            }
        }

        if (currentName.isEmpty()) {
            currentName = name;
        }

        if (currentDescriptor == null || currentDescriptor.isEmpty()) {
            currentDescriptor = descriptor;
        }

        super.visitMethodInsn(currentOpcode, currentOwner, currentName, currentDescriptor, isInterface);
    }

    @Override
    public void visitLdcInsn(Object value) {
        Object currentValue = value;

        if (infos.LDC.containsKey(this.className)) {
            Map<Object, Object> map = infos.LDC.get(this.className);

            if (map.containsKey(value)) {
                currentValue = map.get(value);
            }
        }

        super.visitLdcInsn(currentValue);
    }
}
