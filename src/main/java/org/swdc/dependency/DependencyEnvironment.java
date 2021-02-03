package org.swdc.dependency;

import java.util.Collection;

public interface DependencyEnvironment extends DependencyContext, AutoCloseable {

    Collection<DependencyScope> getScopes();

    DependencyScope getScope(Class scope);

    void registerScope(DependencyScope scope);

}
