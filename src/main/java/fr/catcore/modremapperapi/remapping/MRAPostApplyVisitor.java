package fr.catcore.modremapperapi.remapping;

import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrClass;
import org.objectweb.asm.ClassVisitor;

public class MRAPostApplyVisitor implements TinyRemapper.ApplyVisitorProvider {
    private VisitorInfos infos;
    @Override
    public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next) {
        final String className = cls.getName();
        return new MRAClassVisitor(next, infos, className);
    }

    public void setInfos(VisitorInfos infos) {
        this.infos = infos;
    }
}
