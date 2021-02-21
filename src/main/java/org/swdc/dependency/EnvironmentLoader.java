package org.swdc.dependency;

import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.listeners.AfterRegisterListener;

public interface EnvironmentLoader<T extends DependencyEnvironment> {

    EnvironmentLoader<T> withProvider(Class provider);
    EnvironmentLoader<T> withDeclare(Class declare);
    EnvironmentLoader<T> withScope(DependencyScope scope);
    EnvironmentLoader<T> withComponent(Class component);
    EnvironmentLoader<T> withPackage(String packageName);
    EnvironmentLoader<T> afterRegister(AfterRegisterListener listener);
    EnvironmentLoader<T> afterCreated(AfterCreationListener listener);

    DependencyContext load();

}
