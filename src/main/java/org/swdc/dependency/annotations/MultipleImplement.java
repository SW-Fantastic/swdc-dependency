package org.swdc.dependency.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多实现注解，标记在某一个
 * 抽象类或者接口的具体实现类上面
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultipleImplement {

    /**
     * @return 被实现的抽象类或接口
     */
    Class value();

}
