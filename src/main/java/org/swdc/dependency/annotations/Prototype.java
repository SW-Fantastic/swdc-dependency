package org.swdc.dependency.annotations;

import jakarta.inject.Scope;
import org.swdc.dependency.scopes.PrototypeDependencyScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 非单例Scope，不管理生命周期的Scope类型。
 */
@Scope
@ScopeImplement(value = PrototypeDependencyScope.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Prototype {
}
