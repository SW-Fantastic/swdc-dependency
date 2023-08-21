package org.swdc.dependency.listeners;

import org.swdc.dependency.annotations.Order;
import org.swdc.ours.common.annotations.AnnotationDescription;
import org.swdc.ours.common.annotations.Annotations;

public interface DependencyListener <T> {

    /**
     * 组件创建完毕后执行的方法
     * 此方法允许修改，并且需要返回组件本身。
     * @param object
     * @return
     */
    T afterCreated(T object);

    /**
     * 获取组件的顺序（order注解提供）
     * @return
     */
    default int getOrder() {
        AnnotationDescription desc = Annotations.findAnnotation(this.getClass(), Order.class);
        if (desc == null) {
            return 0;
        }
        return desc.getProperty(int.class,"value");
    }
}
