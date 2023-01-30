package org.swdc.dependency.scopes;

import org.swdc.dependency.AbstractDependencyScope;
import org.swdc.dependency.annotations.Prototype;

import java.util.Collections;
import java.util.List;

public class PrototypeDependencyScope extends AbstractDependencyScope {

    @Override
    public <T> T getByClass(Class<T> clazz) {
        return null;
    }

    @Override
    public <T> T getByName(String name) {
        return null;
    }

    @Override
    public <T> List<T> getByAbstract(Class<T> parent) {
        return Collections.emptyList();
    }

    @Override
    public List<Object> getAllComponent() {
        return Collections.emptyList();
    }

    @Override
    public Class getScopeType() {
        return Prototype.class;
    }

    @Override
    public <T> T put(String name, Class clazz, Class multiple, T component) {
        return component;
    }

    @Override
    public <T> T put(String name, Class clazz, T component) {
        return component;
    }

}
