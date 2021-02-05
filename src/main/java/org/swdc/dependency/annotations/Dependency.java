package org.swdc.dependency.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模块声明注解。
 *
 * 标记此注解的类应该遵守以下约定：
 *
 * 含有一个空白的构造方法或无构造方法。
 *
 * 不含有字段注入。
 *
 * 声明组件需要Factory注解，
 * 请将它标记在生成组件的方法上面。
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dependency {
}
