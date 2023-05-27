package fr.catcore.modremapperapi.remapping;

import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.FieldDef;
import net.fabricmc.mapping.tree.MethodDef;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MixinMethodVisitor extends MethodVisitor {
    private final MixinExtraVisitor extraVisitor;
    protected MixinMethodVisitor(MethodVisitor methodVisitor, MixinExtraVisitor extraVisitor) {
        super(Opcodes.ASM9, methodVisitor);
        this.extraVisitor = extraVisitor;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        if (this.extraVisitor.fields.contains(name) && owner.replace(".", "/").equals(this.extraVisitor.className)) {
            for (ClassDef cl : this.extraVisitor.classDefs) {
                boolean bol = false;
                if (this.extraVisitor.supers.contains(cl.getName("official"))
                        || this.extraVisitor.supers.contains(cl.getName("intermediary"))) {
                    for (FieldDef fl : cl.getFields()) {
                        if (fl.getName("official").equals(name)
                                && (fl.getDescriptor("intermediary").equals(descriptor)
                                || fl.getDescriptor("official").equals(descriptor))) {
                            name = fl.getName("intermediary");
                            descriptor = fl.getDescriptor("intermediary");
                            bol = true;
                            break;
                        }
                    }
                }

                if (bol) break;
            }
        }

        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (this.extraVisitor.methods.contains(name) && owner.replace(".", "/").equals(this.extraVisitor.className)) {
            for (ClassDef cl : this.extraVisitor.classDefs) {
                boolean bol = false;
                if (this.extraVisitor.supers.contains(cl.getName("official"))
                        || this.extraVisitor.supers.contains(cl.getName("intermediary"))) {
                    for (MethodDef fl : cl.getMethods()) {
                        if (fl.getName("official").equals(name)
                                && (fl.getDescriptor("intermediary").equals(descriptor)
                                || fl.getDescriptor("official").equals(descriptor))) {
                            name = fl.getName("intermediary");
                            descriptor = fl.getDescriptor("intermediary");
                            bol = true;
                            break;
                        }
                    }
                }

                if (bol) break;
            }
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
