package org.swdc.dependency.registry;

public class FactoryDependencyInfo {

    private boolean nonStatic;

    private ComponentInfo[] dependencyInfos;

    public FactoryDependencyInfo(ComponentInfo[] info, boolean isStatic) {
        this.dependencyInfos = info;
        this.nonStatic = !isStatic;
    }

    public ComponentInfo[] getDependencies() {
        return dependencyInfos;
    }

    public boolean isStatic() {
        return !nonStatic;
    }
}
