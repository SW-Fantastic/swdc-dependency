package org.swdc.dependency.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 目前仅用于listener和AOP的切面方法，
 * 可以将listener进行排序。
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {

    int value() default 0;

}
