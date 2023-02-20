package org.swdc.dependency.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注可被AOP拦截的类
 * 如果被标记在注解上，则表示此注解为Aspect注解。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface With {

    /**
     * 在本类的对象上面使用这些增强。
     */
    Class[] aspectBy();

}
