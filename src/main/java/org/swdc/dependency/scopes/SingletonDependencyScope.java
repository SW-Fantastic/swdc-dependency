package org.swdc.dependency.scopes;

import jakarta.inject.Singleton;
import org.swdc.dependency.AbstractDependencyScope;

public class SingletonDependencyScope extends AbstractDependencyScope {

    @Override
    public Class getScopeType() {
        return Singleton.class;
    }

}
