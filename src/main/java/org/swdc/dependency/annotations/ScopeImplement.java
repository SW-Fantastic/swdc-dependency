package org.swdc.dependency.annotations;

import org.swdc.dependency.DependencyScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScopeImplement {

    Class<? extends DependencyScope> value();

}
