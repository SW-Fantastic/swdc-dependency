package org.swdc.dependency.application;

import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.EnvironmentLoader;
import org.swdc.dependency.layer.Layer;

public interface SWApplication {

    void onConfig(EnvironmentLoader loader);

    void onLaunch(Layer layer);

    void onStarted(DependencyContext context);

    void onShutdown(DependencyContext context);

}
