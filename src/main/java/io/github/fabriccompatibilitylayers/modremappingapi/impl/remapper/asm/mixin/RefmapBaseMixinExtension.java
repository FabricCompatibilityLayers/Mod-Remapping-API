package io.github.fabriccompatibilitylayers.modremappingapi.impl.remapper.asm.mixin;

import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrEnvironment;
import net.fabricmc.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.tinyremapper.extension.mixin.hard.ImprovedHardTargetMixinClassVisitor;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassVisitor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ApiStatus.Internal
public class RefmapBaseMixinExtension implements TinyRemapper.Extension {
    private final Map<Integer, Collection<Consumer<CommonData>>> tasks;
    private final Predicate<InputTag> inputTagFilter;

    public RefmapBaseMixinExtension(Predicate<InputTag> inputTagFilter) {
        this.tasks = new ConcurrentHashMap<>();
        this.inputTagFilter = inputTagFilter;
    }

    @Override
    public void attach(TinyRemapper.Builder builder) {
        builder.extraAnalyzeVisitor(new AnalyzeVisitorProvider()).extraStateProcessor(this::stateProcessor);
    }

    private void stateProcessor(TrEnvironment environment) {
        var data = new CommonData(environment);

        for (var task : tasks.getOrDefault(environment.getMrjVersion(), Collections.emptyList())) {
            try {
                task.accept(data);
            } catch (RuntimeException e) {
                environment.getLogger().error(e.getMessage());
            }
        }
    }

    private final class AnalyzeVisitorProvider implements TinyRemapper.AnalyzeVisitorProvider {
        @Override
        public ClassVisitor insertAnalyzeVisitor(int mrjVersion, String className, ClassVisitor next) {
            return new ImprovedHardTargetMixinClassVisitor(tasks.computeIfAbsent(mrjVersion, k -> new ConcurrentLinkedQueue<>()), next);
        }

        @Override
        public ClassVisitor insertAnalyzeVisitor(int mrjVersion, String className, ClassVisitor next, InputTag[] inputTags) {
            if (inputTagFilter == null || inputTags == null) {
                return insertAnalyzeVisitor(mrjVersion, className, next);
            } else {
                for (var tag : inputTags) {
                    if (inputTagFilter.test(tag)) {
                        return insertAnalyzeVisitor(mrjVersion, className, next);
                    }
                }

                return next;
            }
        }

        @Override
        public ClassVisitor insertAnalyzeVisitor(boolean isInput, int mrjVersion, String className, ClassVisitor next, InputTag[] inputTags) {
            if (!isInput) {
                return next;
            }

            return insertAnalyzeVisitor(mrjVersion, className, next, inputTags);
        }
    }
}
