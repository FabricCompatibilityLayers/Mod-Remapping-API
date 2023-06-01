package fr.catcore.modremapperapi.remapping;

import net.fabricmc.mappingio.tree.MappingTree;
import org.objectweb.asm.*;

import java.util.List;

public class MixinExtraVisitor extends ClassVisitor {
    protected final List<MappingTree.ClassMapping> classDefs;
    protected final List<String> supers, fields, methods;
    protected String className = "";

    public MixinExtraVisitor(ClassVisitor next, List<MappingTree.ClassMapping> classDefs,
                             List<String> supers, List<String> fields, List<String> methods) {
        super(Opcodes.ASM9, next);
        this.classDefs = classDefs;
        this.supers = supers;
        this.fields = fields;
        this.methods = methods;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        RemapUtil.MIXINED.put(name.replace(".", "/"), this.supers);
        this.className = name.replace(".", "/");
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (this.fields.contains(name)) {
            for (MappingTree.ClassMapping cl : this.classDefs) {
                boolean bol = false;
                if (this.supers.contains(cl.getName("official"))
                        || this.supers.contains(cl.getName("intermediary"))) {
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

        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (this.methods.contains(name)) {
            for (MappingTree.ClassMapping cl : this.classDefs) {
                boolean bol = false;
                if (this.supers.contains(cl.getName("official"))
                        || this.supers.contains(cl.getName("intermediary"))) {
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

        return new MixinMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions), this);
    }
}
