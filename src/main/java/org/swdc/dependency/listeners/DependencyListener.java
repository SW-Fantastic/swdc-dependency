package org.swdc.dependency.listeners;

import org.swdc.dependency.annotations.Order;
import org.swdc.dependency.utils.AnnotationDescription;
import org.swdc.dependency.utils.AnnotationUtil;

public interface DependencyListener <T> {

    T afterCreated(T object);

    default int getOrder() {
        AnnotationDescription desc = AnnotationUtil.findAnnotation(this.getClass(), Order.class);
        if (desc == null) {
            return 0;
        }
        return desc.getProperty(int.class,"value");
    }
}
