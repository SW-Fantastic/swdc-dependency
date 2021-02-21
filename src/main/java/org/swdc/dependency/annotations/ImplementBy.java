package org.swdc.dependency.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 抽象注解。
 * 标注在抽象类，或接口上面。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ImplementBy {

    /**
     * 具体的实现类。
     * @return
     */
    Class[] value();

}
