package org.swdc.dependency.annotations;

import org.swdc.dependency.DependencyScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Scope的实现注解。
 * 标记在自定义的Scope注解上面，
 * 用来指定自定义的Scope注解的实现类。
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScopeImplement {

    /**
     * Scope的实现类
     * @return Scope的具体实现类
     */
    Class value();

}
