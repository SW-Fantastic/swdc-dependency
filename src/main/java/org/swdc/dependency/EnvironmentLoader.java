package org.swdc.dependency;

import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.listeners.AfterRegisterListener;

public interface EnvironmentLoader<T extends DependencyContext> {

    EnvironmentLoader<T> withProvider(Class provider);
    EnvironmentLoader<T> withDeclare(Class declare);
    EnvironmentLoader<T> withScope(DependencyScope scope);
    EnvironmentLoader<T> withComponent(Class component);
    <C> EnvironmentLoader<T> withInstance(Class<C> clazz, C instance);
    EnvironmentLoader<T> withPackage(String packageName);
    EnvironmentLoader<T> layerExport(Class clazz);
    EnvironmentLoader<T> layerExport(String name);
    EnvironmentLoader<T> layerExportAbstract(Class clazz);
    EnvironmentLoader<T> afterRegister(AfterRegisterListener listener);
    EnvironmentLoader<T> afterCreated(AfterCreationListener listener);

    DependencyContext load();

}
