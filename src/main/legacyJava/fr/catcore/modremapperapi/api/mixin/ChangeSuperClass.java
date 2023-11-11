package fr.catcore.modremapperapi.api.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Change the super class of the annotated class.
 * @deprecated : Use {@link fr.catcore.cursedmixinextensions.annotations.ChangeSuperClass} instead
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangeSuperClass {
    /**
     * The super class to use.
     */
    public Class<?> value();
}
