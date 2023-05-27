package fr.catcore.modremapperapi.remapping;

import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.FieldDef;
import net.fabricmc.mapping.tree.MethodDef;
import org.objectweb.asm.*;

import java.util.List;

public class MixinExtraVisitor extends ClassVisitor {
    protected final List<ClassDef> classDefs;
    protected final List<String> supers, fields, methods;
    protected String className = "";

    public MixinExtraVisitor(ClassVisitor next, List<ClassDef> classDefs,
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
            for (ClassDef cl : this.classDefs) {
                boolean bol = false;
                if (this.supers.contains(cl.getName("official"))
                        || this.supers.contains(cl.getName("intermediary"))) {
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

        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (this.methods.contains(name)) {
            for (ClassDef cl : this.classDefs) {
                boolean bol = false;
                if (this.supers.contains(cl.getName("official"))
                        || this.supers.contains(cl.getName("intermediary"))) {
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

        return new MixinMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions), this);
    }
}
