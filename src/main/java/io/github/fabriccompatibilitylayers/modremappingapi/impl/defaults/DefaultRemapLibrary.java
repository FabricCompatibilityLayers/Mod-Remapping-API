package io.github.fabriccompatibilitylayers.modremappingapi.impl.defaults;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.RemapLibrary;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

@ApiStatus.Internal
public record DefaultRemapLibrary(
        @Nullable String url,
        @Nullable Path path,
        String fileName,
        Set<String> toExclude,
        boolean mergeWithMainJar,
        int priority
) implements RemapLibrary {
    @Override
    public @Nullable String getURL() {
        return this.url;
    }

    @Override
    public @Nullable Path getPath() {
        return this.path;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public List<String> getToExclude() {
        return this.toExclude.stream().toList();
    }

    @Override
    public boolean mergeWithMainJar() {
        return this.mergeWithMainJar;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public static final class BuilderImpl implements Builder {
        private final String fileName;
        private @Nullable String url;
        private @Nullable Path path;
        private final Set<String> toExclude = new HashSet<>();
        private boolean mergeWithMainJar = false;
        private int priority = 1000;

        public BuilderImpl(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public Builder url(String url) {
            this.path = null;
            this.url = url;
            return this;
        }

        @Override
        public Builder path(Path path) {
            this.url = null;
            this.path = path;
            return this;
        }

        @Override
        public Builder addExcludedPaths(Collection<String> toExclude) {
            this.toExclude.addAll(toExclude);
            return this;
        }

        @Override
        public Builder mergeWithMainJar(boolean mergeWithMainJar) {
            this.mergeWithMainJar = mergeWithMainJar;
            return this;
        }

        @Override
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public RemapLibrary build() {
            return new DefaultRemapLibrary(url, path, fileName, toExclude, mergeWithMainJar, priority);
        }
    }
}
