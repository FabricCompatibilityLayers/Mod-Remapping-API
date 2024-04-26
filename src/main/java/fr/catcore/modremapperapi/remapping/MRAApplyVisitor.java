package fr.catcore.modremapperapi.remapping;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.VisitorInfosImpl;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrClass;
import org.objectweb.asm.ClassVisitor;

public class MRAApplyVisitor implements TinyRemapper.ApplyVisitorProvider {
    private VisitorInfosImpl infos;

    @Override
    public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next) {
        final String className = cls.getName();
        return new MRAClassVisitor(next, infos, className);
    }

    public void setInfos(VisitorInfosImpl infos) {
        this.infos = infos;
    }
}
