package fr.catcore.modremapperapi.remapping;

import net.fabricmc.mappingio.tree.MappingTree;
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
            for (MappingTree.ClassMapping cl : this.extraVisitor.classDefs) {
                boolean bol = false;
                if (this.extraVisitor.supers.contains(cl.getName("official"))
                        || this.extraVisitor.supers.contains(cl.getName("intermediary"))) {
                    for (MappingTree.FieldMapping fl : cl.getFields()) {
                        if (fl.getName("official").equals(name)
                                && (fl.getDesc("intermediary").equals(descriptor)
                                || fl.getDesc("official").equals(descriptor))) {
                            name = fl.getName("intermediary");
                            descriptor = fl.getDesc("intermediary");
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
            for (MappingTree.ClassMapping cl : this.extraVisitor.classDefs) {
                boolean bol = false;
                if (this.extraVisitor.supers.contains(cl.getName("official"))
                        || this.extraVisitor.supers.contains(cl.getName("intermediary"))) {
                    for (MappingTree.MethodMapping fl : cl.getMethods()) {
                        if (fl.getName("official").equals(name)
                                && (fl.getDesc("intermediary").equals(descriptor)
                                || fl.getDesc("official").equals(descriptor))) {
                            name = fl.getName("intermediary");
                            descriptor = fl.getDesc("intermediary");
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
