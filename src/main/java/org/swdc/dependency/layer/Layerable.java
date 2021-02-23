package org.swdc.dependency.layer;

import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.registry.ComponentInfo;

import java.util.List;

public interface Layerable extends DependencyContext {

    List<ComponentInfo> getExport();

    boolean contains(Class clazz);

    void setImport(Layer layer);

    void exportByName(String name);

    void exportByClass(Class clazz);

    void exportByAbstract(Class clazz);

}
