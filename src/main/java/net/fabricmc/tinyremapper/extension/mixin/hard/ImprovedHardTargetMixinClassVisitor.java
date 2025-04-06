package net.fabricmc.tinyremapper.extension.mixin.hard;

import net.fabricmc.tinyremapper.extension.mixin.common.MapUtility;
import net.fabricmc.tinyremapper.extension.mixin.common.data.*;
import net.fabricmc.tinyremapper.extension.mixin.hard.annotation.ImplementsAnnotationVisitor;
import net.fabricmc.tinyremapper.extension.mixin.hard.data.SoftInterface;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.*;
import java.util.function.Consumer;

public class ImprovedHardTargetMixinClassVisitor extends ClassVisitor {
    private final Collection<Consumer<CommonData>> tasks;
    private MxClass _class;

    // @Mixin
    private final List<String> targets = new ArrayList<>();

    // @Implements
    private final List<SoftInterface> interfaces = new ArrayList<>();

    public ImprovedHardTargetMixinClassVisitor(Collection<Consumer<CommonData>> tasks, ClassVisitor delegate) {
        super(Constant.ASM_VERSION, delegate);
        this.tasks = Objects.requireNonNull(tasks);
    }

    /**
     * This is called before visitAnnotation.
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this._class = new MxClass(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * This is called before visitMethod & visitField.
     */
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);

        if (Annotation.MIXIN.equals(descriptor)) {
            av = new ImprovedMixinAnnotationVisitor(av, targets, this._class);
        } else if (Annotation.IMPLEMENTS.equals(descriptor)) {
            av = new ImplementsAnnotationVisitor(av, interfaces);
        }

        return av;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
        MxMember field = _class.getField(name, descriptor);

        if (targets.isEmpty()) {
            return fv;
        } else {
            return new HardTargetMixinFieldVisitor(tasks, fv, field, Collections.unmodifiableList(targets));
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        MxMember method = _class.getMethod(name, descriptor);

        if (!interfaces.isEmpty() && !MapUtility.IGNORED_NAME.contains(name)) {
            ImplementsAnnotationVisitor.visitMethod(tasks, method, interfaces);
        }

        if (targets.isEmpty()) {
            return mv;
        } else {
            return new HardTargetMixinMethodVisitor(tasks, mv, method, Collections.unmodifiableList(targets));
        }
    }
}
