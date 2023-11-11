package fr.catcore.modremapperapi.api.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Turn annotated method into a constructor method from super class.
 * Replace all references to it then remove the annotated method.
 * @deprecated : Use {@link fr.catcore.cursedmixinextensions.annotations.ShadowSuperConstructor} instead
 */
@Deprecated
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SuperConstructor {
}
