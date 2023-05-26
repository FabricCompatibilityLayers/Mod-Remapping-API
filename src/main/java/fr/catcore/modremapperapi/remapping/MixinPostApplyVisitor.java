package fr.catcore.modremapperapi.remapping;

import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrClass;
import org.objectweb.asm.ClassVisitor;

public class MixinPostApplyVisitor implements TinyRemapper.ApplyVisitorProvider{
    @Override
    public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next) {
        return new MixinExtraVisitor(next);
    }
}
