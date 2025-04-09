package io.github.fabriccompatibilitylayers.modremappingapi.api.v2;

import org.jetbrains.annotations.Nullable;

public interface VisitorInfos {
    void registerSuperType(String target, String replacement);

    void registerTypeAnnotation(String target, String replacement);

    void registerMethodTypeIns(String target, String replacement);

    void registerFieldRef(String targetClass, String targetField, String targetDesc, FullClassMember classMember);

    void registerMethodInvocation(String targetClass, String targetMethod, String targetDesc, FullClassMember classMember);

    void registerLdc(String targetClass, Object targetLdc, Object replacement);

    void registerInstantiation(String target, String replacement);


    static FullClassMember classMember(String owner, String name, @Nullable String desc, @Nullable Boolean isStatic) {
        return new io.github.fabriccompatibiltylayers.modremappingapi.api.v1.VisitorInfos.FullClassMember(owner, name, desc, isStatic);
    }

    static FullClassMember classMember(String owner, String name, @Nullable Boolean isStatic) {
        return new io.github.fabriccompatibiltylayers.modremappingapi.api.v1.VisitorInfos.FullClassMember(owner, name, isStatic);
    }

    interface FullClassMember extends MappingUtils.ClassMember {
        String getOwner();
        @Nullable Boolean isStatic();
    }
}
