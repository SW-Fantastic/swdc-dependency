package org.swdc.dependency.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AliasFor {

    /**
     * 是哪个注解的别名
     * @return 注解类
     */
    Class annotation();

    /**
     * 属性名
     * @return 是注解的那个属性的别名
     */
    String value();

}
