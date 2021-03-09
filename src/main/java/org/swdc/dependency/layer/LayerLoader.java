package org.swdc.dependency.layer;

import org.swdc.dependency.DependencyContext;

public interface LayerLoader {

    void setEnvironmentModule(Module module);

    DependencyContext load();

}
