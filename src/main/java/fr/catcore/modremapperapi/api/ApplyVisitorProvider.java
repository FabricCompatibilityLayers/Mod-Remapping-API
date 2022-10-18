package fr.catcore.modremapperapi.api;

import org.objectweb.asm.ClassVisitor;

public interface ApplyVisitorProvider {
    ClassVisitor insertApplyVisitor(String trClassName, ClassVisitor next);
}
