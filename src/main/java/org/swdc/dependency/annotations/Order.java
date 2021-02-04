package org.swdc.dependency.annotations;

/**
 * 目前仅用于listener，
 * 可以将listener进行排序。
 */
public @interface Order {

    int value() default 0;

}
