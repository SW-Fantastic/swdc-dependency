package org.swdc.dependency.registry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DependencyInfo {

    private Field field;
    private Method setter;
    private ComponentInfo[] dependency;

    public DependencyInfo(Field field, ComponentInfo info) {
        this.dependency = new ComponentInfo[]{ info };
        this.field = field;
    }

    public DependencyInfo(Method method, ComponentInfo[] infos) {
        this.dependency = infos;
        this.setter = method;
    }

    public ComponentInfo[] getDependency() {
        return dependency;
    }

    public Field getField() {
        return field;
    }

    public Method getSetter() {
        return setter;
    }
}
