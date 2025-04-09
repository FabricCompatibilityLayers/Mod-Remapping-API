package io.github.fabriccompatibiltylayers.modremappingapi.impl;

import io.github.fabriccompatibilitylayers.modremappingapi.api.v2.VisitorInfos;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public class VisitorInfosImpl extends fr.catcore.modremapperapi.remapping.VisitorInfos implements VisitorInfos, io.github.fabriccompatibiltylayers.modremappingapi.api.v1.VisitorInfos {
    public final Map<String, String> SUPERS = new HashMap<>();
    public final Map<String, String> ANNOTATION = new HashMap<>();
    public final Map<String, String> METHOD_TYPE = new HashMap<>();

    public final Map<String, String> INSTANTIATION = new HashMap<>();
    public final Map<String, Map<String, Map<String, VisitorInfos.FullClassMember>>> METHOD_INVOCATION = new HashMap<>();
    public final Map<String, Map<String, Map<String, VisitorInfos.FullClassMember>>> FIELD_REF = new HashMap<>();
    public final Map<String, Map<Object, Object>> LDC = new HashMap<>();

    @Override
    public void registerSuperType(String target, String replacement) {
        SUPERS.put(target, replacement);
    }

    @Override
    public void registerTypeAnnotation(String target, String replacement) {
        ANNOTATION.put(target, replacement);
    }

    @Override
    public void registerMethodTypeIns(String target, String replacement) {
        METHOD_TYPE.put(target, replacement);
    }

    @Override
    public void registerFieldRef(String targetClass, String targetField, String targetDesc, io.github.fabriccompatibiltylayers.modremappingapi.api.v1.VisitorInfos.FullClassMember classMember) {
        this.registerFieldRef(targetClass, targetField, targetDesc, (VisitorInfos.FullClassMember) classMember);
    }

    @Override
    public void registerMethodInvocation(String targetClass, String targetMethod, String targetDesc, io.github.fabriccompatibiltylayers.modremappingapi.api.v1.VisitorInfos.FullClassMember classMember) {
        this.registerMethodInvocation(targetClass, targetMethod, targetDesc, (VisitorInfos.FullClassMember) classMember);
    }

    @Override
    public void registerFieldRef(String targetClass, String targetField, String targetDesc, VisitorInfos.FullClassMember classMember) {
        FIELD_REF.computeIfAbsent(targetClass, k -> new HashMap<>())
                .computeIfAbsent(targetField, k -> new HashMap<>())
                .put(targetDesc, classMember);
    }

    @Override
    public void registerMethodInvocation(String targetClass, String targetMethod, String targetDesc, VisitorInfos.FullClassMember classMember) {
        METHOD_INVOCATION.computeIfAbsent(targetClass, k -> new HashMap<>())
                .computeIfAbsent(targetMethod, k -> new HashMap<>())
                .put(targetDesc, classMember);
    }

    @Override
    public void registerLdc(String targetClass, Object targetLdc, Object replacement) {
        LDC.computeIfAbsent(targetClass, k -> new HashMap<>())
                .put(targetLdc, replacement);
    }

    @Override
    public void registerInstantiation(String target, String replacement) {
        INSTANTIATION.put(target, replacement);
    }

    // Backward compatibility zone
    @Deprecated
    public void registerSuperType(Type target, Type replacement) {
        registerSuperType(target.type, replacement.type);
    }

    @Deprecated
    public void registerTypeAnnotation(Type target, Type replacement) {
        registerTypeAnnotation(target.type, replacement.type);
    }

    @Deprecated
    public void registerMethodTypeIns(Type target, Type replacement) {
        registerMethodTypeIns(target.type, replacement.type);
    }

    @Deprecated
    public void registerMethodFieldIns(MethodNamed target, MethodNamed replacementObject) {
        registerMethodNamed(target, replacementObject, FIELD_REF);
    }

    @Deprecated
    public void registerMethodMethodIns(MethodNamed target, MethodNamed replacementObject) {
        registerMethodNamed(target, replacementObject, METHOD_INVOCATION);
    }

    @Deprecated
    public void registerMethodLdcIns(MethodValue target, MethodValue replacement) {
        LDC.computeIfAbsent(target.owner, k -> new HashMap<>())
                .put(target.value, replacement.value);
    }

    private void registerMethodNamed(MethodNamed target, MethodNamed replacementObject, Map<String, Map<String, Map<String, VisitorInfos.FullClassMember>>> map) {
        String targetClass = target.owner;
        String targetMember = target.name;

        String replacement = replacementObject.name;
        if (replacement == null) replacement = "";

        VisitorInfos.FullClassMember classMember = new io.github.fabriccompatibiltylayers.modremappingapi.api.v1.VisitorInfos.FullClassMember(replacementObject.owner, replacement, null, null);

        if (targetMember == null) targetMember = "";

        map.computeIfAbsent(targetClass, k -> new HashMap<>())
                .computeIfAbsent(targetMember, k -> new HashMap<>())
                .put("", classMember);
    }
}
