package fr.catcore.modremapperapi.api.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Turn annotated method into a <init> method.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ShadowConstructor {
}
