package io.github.fabriccompatibiltylayers.modremappingapi.api.v1;

import io.github.fabriccompatibiltylayers.modremappingapi.api.MappingUtils;
import org.jetbrains.annotations.Nullable;

public interface VisitorInfos {
    void registerSuperType(String target, String replacement);

    void registerTypeAnnotation(String target, String replacement);

    void registerMethodTypeIns(String target, String replacement);

    void registerFieldRef(String targetClass, String targetField, String targetDesc, FullClassMember classMember);

    void registerMethodInvocation(String targetClass, String targetMethod, String targetDesc, FullClassMember classMember);

    void registerLdc(String targetClass, Object targetLdc, Object replacement);

    void registerInstantiation(String target, String replacement);

    class FullClassMember extends MappingUtils.ClassMember implements io.github.fabriccompatibilitylayers.modremappingapi.api.v2.VisitorInfos.FullClassMember {
        public final String owner;
        public final @Nullable Boolean isStatic;

        public FullClassMember(String owner, String name, @Nullable String desc, @Nullable Boolean isStatic) {
            super(name, desc);
            this.owner = owner;
            this.isStatic = isStatic;
        }

        public FullClassMember(String owner, String name, @Nullable Boolean isStatic) {
            this(owner, name, null, isStatic);
        }

        @Override
        public String getOwner() {
            return this.owner;
        }

        @Override
        public @Nullable Boolean isStatic() {
            return this.isStatic;
        }
    }
}
