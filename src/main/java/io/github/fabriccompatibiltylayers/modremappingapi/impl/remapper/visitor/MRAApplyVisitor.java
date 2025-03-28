package io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.visitor;

import io.github.fabriccompatibiltylayers.modremappingapi.impl.VisitorInfosImpl;
import io.github.fabriccompatibiltylayers.modremappingapi.impl.remapper.asm.MRAClassVisitor;
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
