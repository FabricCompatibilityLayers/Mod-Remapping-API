package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.visitor;

import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.VisitorInfosImpl;
import io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.asm.MRAClassVisitor;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrClass;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassVisitor;

@ApiStatus.Internal
public class MRAApplyVisitor implements TinyRemapper.ApplyVisitorProvider {
    private final VisitorInfosImpl infos;

    public MRAApplyVisitor(VisitorInfosImpl infos) {
        this.infos = infos;
    }

    @Override
    public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next) {
        final String className = cls.getName();
        return new MRAClassVisitor(next, infos, className);
    }
}
