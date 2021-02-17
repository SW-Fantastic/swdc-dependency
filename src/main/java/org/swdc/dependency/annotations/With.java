package org.swdc.dependency.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注可被AOP拦截的类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface With {

    /**
     * 在本类的对象上面使用这些增强。
     */
    Class[] aspectBy();

}
