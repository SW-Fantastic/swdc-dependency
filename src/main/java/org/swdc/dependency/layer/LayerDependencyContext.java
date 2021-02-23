package org.swdc.dependency.layer;

import org.swdc.dependency.DependencyContext;

import java.util.List;

public class LayerDependencyContext implements DependencyContext {

    private Layer layer;

    public LayerDependencyContext(Layer layer) {
        this.layer = layer;
    }

    @Override
    public <T> T getByClass(Class<T> clazz) {
        return layer.findParentByClass(clazz);
    }

    @Override
    public <T> T getByName(String name) {
        return layer.findParentByName(name);
    }

    @Override
    public <T> List<T> getByAbstract(Class<T> parent) {
        return layer.findParentByAbstract(parent);
    }

    @Override
    public List<Object> getAllComponent() {
        return layer.getLayerable().getAllComponent();
    }
}
