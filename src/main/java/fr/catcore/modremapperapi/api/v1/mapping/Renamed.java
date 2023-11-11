package fr.catcore.modremapperapi.api.v1.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
        ElementType.METHOD,
        ElementType.TYPE,
        ElementType.FIELD,
        ElementType.PACKAGE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface Renamed {
    public String oldName();
}
