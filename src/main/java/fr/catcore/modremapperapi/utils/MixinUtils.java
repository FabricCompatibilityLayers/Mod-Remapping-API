package fr.catcore.modremapperapi.utils;

import fr.catcore.modremapperapi.api.mixin.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.util.Annotations;

import java.util.ListIterator;
import java.util.function.Consumer;

/**
 * @author LlamaLad7
 */
public class MixinUtils {
    public static void applyASMMagic(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        AnnotationNode changeSuperClass = Annotations.getInvisible(targetClass, ChangeSuperClass.class);

        if (changeSuperClass != null) {
            String oldOwner = targetClass.superName;
            targetClass.superName = Annotations.<Type>getValue(changeSuperClass).getInternalName();
            transformCalls(targetClass, call -> {
                if (call.getOpcode() == Opcodes.INVOKESPECIAL && call.owner.equals(oldOwner)) {
                    call.owner = targetClass.superName;
                }
            });
        }

        for (ListIterator<MethodNode> it = targetClass.methods.listIterator(); it.hasNext(); ) {
            MethodNode method = it.next();

            if (Annotations.getInvisible(method, SuperConstructor.class) != null) {
                it.remove();
                transformCalls(targetClass, call -> {
                    if (call.name.equals(method.name) && call.desc.equals(method.desc)) {
                        call.setOpcode(Opcodes.INVOKESPECIAL);
                        call.name = "<init>";
                        call.owner = targetClass.superName;
                    }
                });
                continue;
            }

            if (Annotations.getInvisible(method, ShadowConstructor.class) != null) {
                it.remove();
                transformCalls(targetClass, call -> {
                    if (call.name.equals(method.name) && call.desc.equals(method.desc)) {
                        call.setOpcode(Opcodes.INVOKESPECIAL);
                        call.name = "<init>";
                    }
                });
                continue;
            }

            if (Annotations.getInvisible(method, Public.class) != null) {
                method.access |= Opcodes.ACC_PUBLIC;
                method.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
            }

            if (Annotations.getInvisible(method, NewConstructor.class) != null) {
                method.name = "<init>";
            }
        }

        for (ListIterator<FieldNode> it = targetClass.fields.listIterator(); it.hasNext(); ) {
            FieldNode field = it.next();

            if (Annotations.getInvisible(field, Public.class) != null) {
                field.access |= Opcodes.ACC_PUBLIC;
                field.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
            }
        }
    }

    private static void transformCalls(ClassNode classNode, Consumer<MethodInsnNode> consumer) {
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    consumer.accept(call);
                }
            }
        }
    }
}
