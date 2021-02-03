package org.swdc.dependency.registry;

import java.lang.reflect.Constructor;

public class ConstructorInfo {

    private Constructor constructor;
    private ComponentInfo[] dependencies;

    public ConstructorInfo(Constructor constructor, ComponentInfo[] dependencies) {
        this.constructor = constructor;
        this.dependencies = dependencies;
    }

    public ComponentInfo[] getDependencies() {
        return dependencies;
    }

    public Constructor getConstructor() {
        return constructor;
    }
}
