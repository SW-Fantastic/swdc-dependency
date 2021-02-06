package org.swdc.dependency;

import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.listeners.AfterRegisterListener;

import java.util.Collection;

public interface DependencyEnvironment extends DependencyContext, AutoCloseable {

    Collection<DependencyScope> getScopes();

    DependencyScope getScope(Class scope);

    void registerScope(DependencyScope scope);

    void registerComponent(Class component);

    void registerCreationListener(AfterCreationListener listener);

    void registerParsedListener(AfterRegisterListener registerListener);

}
