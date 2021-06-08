package org.swdc.dependency.layer;

import org.swdc.dependency.DependencyContext;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class LayerDependencyContext implements DependencyContext, Closeable {

    private Layer layer;

    public LayerDependencyContext(Layer layer) {
        this.layer = layer;
    }

    @Override
    public <T> T getByClass(Class<T> clazz) {
        return layer.findLayersByClass(clazz,null);
    }

    @Override
    public <T> T getByName(String name) {
        return layer.findLayersByName(name,null);
    }

    @Override
    public <T> List<T> getByAbstract(Class<T> parent) {
        return layer.findLayersByAbstract(parent,null);
    }

    @Override
    public List<Object> getAllComponent() {
        return layer.getLayerable().getAllComponent();
    }

    @Override
    public void close() throws IOException {
        try {
            this.layer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
