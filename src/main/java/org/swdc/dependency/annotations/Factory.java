package org.swdc.dependency.annotations;

import jakarta.inject.Singleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 组件声明注解。
 *
 * 使用此注解请遵循以下约定：
 *
 * 方法有返回引用类型的返回值。
 * 方法的参数是可以被注入的，可以在这个位置引用其他组件。
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Factory {

    /**
     * 初始化方法的方法名称。
     * @return 初始化方法名
     */
    String initMethod() default "";

    /**
     * 销毁方法的方法名称
     * @return 销毁方法名
     */
    String destroyMethod() default "";

    /**
     * 是否为某一个接口或抽象类的多种实现之一。
     * @return 被实现的接口或抽象类
     */
    Class multiple() default Object.class;

    /**
     * Scope，作用范围，默认为单例
     * @return 作用范围
     */
    Class scope() default Singleton.class;

}
