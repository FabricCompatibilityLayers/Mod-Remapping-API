package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.asm;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.VisitorInfosImpl;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Optional;

@ApiStatus.Internal
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
        var currentType = type;
        var skip = false;

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
        var currentOwner = owner;
        var currentName = name;
        var currentDescriptor = descriptor;

        if (infos.FIELD_REF.containsKey(owner)) {
            var fields = infos.FIELD_REF.get(owner);

            var args = Optional.ofNullable(fields.get(name))
                    .orElse(fields.get(""));

            if (args != null) {
                var classMember = Optional.ofNullable(args.get(descriptor))
                        .orElse(args.get(""));

                if (classMember != null) {
                    currentOwner = classMember.getOwner();
                    currentName = classMember.getName();
                    currentDescriptor = classMember.getDesc();
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
        var currentOpcode = opcode;
        var currentOwner = owner;
        var currentName = name;
        var currentDescriptor = descriptor;
        var isInterfaceCall = isInterface;

        var skip = false;

        if (opcode == INVOKESPECIAL && infos.INSTANTIATION.containsKey(owner) && name.equals("<init>")) {
            currentOwner = infos.INSTANTIATION.get(owner);
            skip = true;
        }

        if (!skip && (opcode == INVOKEVIRTUAL || opcode == INVOKESTATIC || opcode == INVOKEINTERFACE)) {
            if (infos.METHOD_INVOCATION.containsKey(owner)) {
                var methods = infos.METHOD_INVOCATION.get(owner);

                var args = Optional.ofNullable(methods.get(currentName))
                        .orElse(methods.get(""));

                if (args != null) {
                    var fullClassMember = Optional.ofNullable(args.get(currentDescriptor))
                            .orElse(args.get(""));

                    if (fullClassMember != null) {
                        currentOwner = fullClassMember.getOwner();
                        currentName = fullClassMember.getName();
                        currentDescriptor = fullClassMember.getDesc();

                        if (fullClassMember.isStatic() != null) {
                            currentOpcode = fullClassMember.isStatic() ? INVOKESTATIC : INVOKEVIRTUAL;
                        }
                        
                        if (isInterfaceCall) {
                            isInterfaceCall = false;
                        }
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

        super.visitMethodInsn(currentOpcode, currentOwner, currentName, currentDescriptor, isInterfaceCall);
    }

    @Override
    public void visitLdcInsn(Object value) {
        var currentValue = value;

        if (infos.LDC.containsKey(this.className)) {
            var ldcMap = infos.LDC.get(this.className);
            
            if (ldcMap.containsKey(value)) {
                currentValue = ldcMap.get(value);
            }
        }

        super.visitLdcInsn(currentValue);
    }
}
