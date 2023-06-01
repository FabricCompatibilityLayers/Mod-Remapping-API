package fr.catcore.modremapperapi.remapping;

import fr.catcore.modremapperapi.utils.Constants;
import net.fabricmc.mappingio.tree.MappingTree;
import org.objectweb.asm.*;

import java.util.List;
import java.util.Map;

public class MixinExtraVisitor extends ClassVisitor {
    protected final List<MappingTree.ClassMapping> classDefs;
    protected final List<String> supers;
    protected final Map<String, List<String>> fields, methods;
    protected String className = "";

    public MixinExtraVisitor(ClassVisitor next, List<MappingTree.ClassMapping> classDefs,
                             List<String> supers, Map<String, List<String>> fields, Map<String, List<String>> methods) {
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
        if (this.fields.containsKey(name)) {
            boolean shadowed = name.startsWith("shadow$");
            if (shadowed) {
                name = name.replace("shadow$", "");
            }

            for (MappingTree.ClassMapping cl : this.classDefs) {
                boolean bol = false;
                if (this.supers.contains(cl.getName("official"))
                        || this.supers.contains(cl.getName("intermediary"))) {
                    for (MappingTree.FieldMapping fl : cl.getFields()) {
                        if (fl.getName("official").equals(name)) {
                            Constants.MAIN_LOGGER.info(
                                    "Remapping Shadowed field %s->%s in mixin %s",
                                    name,
                                    fl.getName("intermediary"),
                                    this.className
                            );
                            name = (shadowed ? "shadow$" : "") + fl.getName("intermediary");
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
        if (this.methods.containsKey(name)) {
            boolean shadowed = name.startsWith("shadow$");
            if (shadowed) {
                name = name.replace("shadow$", "");
            }

            for (MappingTree.ClassMapping cl : this.classDefs) {
                boolean bol = false;
                if (this.supers.contains(cl.getName("official"))
                        || this.supers.contains(cl.getName("intermediary"))) {
                    for (MappingTree.MethodMapping fl : cl.getMethods()) {
                        if (fl.getName("official").equals(name)
                                && (fl.getDesc("intermediary").equals(descriptor)
                                || fl.getDesc("official").equals(descriptor))) {
                            Constants.MAIN_LOGGER.info(
                                    "Remapping Shadowed method %s%s->%s%s in mixin %s",
                                    name,
                                    descriptor,
                                    fl.getName("intermediary"),
                                    fl.getDesc("intermediary"),
                                    this.className
                            );
                            name = (shadowed ? "shadow$" : "") + fl.getName("intermediary");
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
