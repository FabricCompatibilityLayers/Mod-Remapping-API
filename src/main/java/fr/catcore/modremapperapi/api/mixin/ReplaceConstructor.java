package fr.catcore.modremapperapi.api.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overwrite for constructor methods.
 * @see org.spongepowered.asm.mixin.Overwrite
 * @deprecated : Use {@link fr.catcore.cursedmixinextensions.annotations.ReplaceConstructor} instead
 */
@Deprecated
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReplaceConstructor {
}
