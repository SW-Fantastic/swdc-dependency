package org.swdc.dependency.annotations;

import jakarta.inject.Singleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Factory {

    String initMethod() default "";

    String destroyMethod() default "";

    Class multiple() default Object.class;

    Class scope() default Singleton.class;

}
