package org.swdc.dependency.annotations;

import org.swdc.dependency.interceptor.AspectAt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记在方法上面，表示此方法是一个切面方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aspect {

    /**
     * 通过正则匹配被AOP的类的方法
     * @return 填写正则表达式
     */
    String byNameRegex() default "";

    /**
     * 通过返回类型匹配被AOP的类的方法
     * @return 填写返回类型
     */
    Class[] byReturnType() default Object.class;

    /**
     * 通过注解匹配被AOP的类的方法
     * @return 填写注解类
     */
    Class byAnnotation() default Object.class;

    /**
     * AOP方法插入的位置
     * @return 填写增强发生的时机
     */
    AspectAt at();

}
