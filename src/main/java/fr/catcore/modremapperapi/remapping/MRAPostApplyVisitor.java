package fr.catcore.modremapperapi.remapping;

import fr.catcore.modremapperapi.api.ApplyVisitorProvider;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrClass;
import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.List;

public class MRAPostApplyVisitor implements TinyRemapper.ApplyVisitorProvider {
    private final List<ApplyVisitorProvider> visitorProviders = new ArrayList<>();
    @Override
    public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next) {
        final String className = cls.getName();

        for (ApplyVisitorProvider visitorProvider : visitorProviders) {
            next = visitorProvider.insertApplyVisitor(className, next);
        }

        return next;
    }

    public void addProvider(ApplyVisitorProvider visitorProvider) {
        this.visitorProviders.add(visitorProvider);
    }
}
