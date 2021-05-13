package org.swdc.dependency.registry;

import org.swdc.dependency.Listenable;
import org.swdc.dependency.listeners.AfterRegisterListener;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultDependencyRegistryContext extends DefaultDependencyRegister implements DependencyRegisterContext,
        Listenable<AfterRegisterListener> {

    private List<AfterRegisterListener> afterRegisterListeners = new ArrayList<>();

    @Override
    public ComponentInfo findByClass(Class clazz) {
        if (clazz.isPrimitive()){
            return null;
        }
        if (Modifier.isAbstract(clazz.getModifiers()) ||
                Modifier.isInterface(clazz.getModifiers())) {
            if (!this.abstractInfo.containsKey(clazz)){
                return defaultInfo.get(clazz);
            }
            return null;
        }
        return defaultInfo.get(clazz);
    }

    @Override
    public ComponentInfo findByNamed(String named) {
        return namedInfo.get(named);
    }

    @Override
    public List<ComponentInfo> findByAbstract(Class clazz) {
        if (!Modifier.isAbstract(clazz.getModifiers()) &&
                !Modifier.isInterface(clazz.getModifiers())) {
            return null;
        }
        return abstractInfo.get(clazz);
    }

    @Override
    public List<ComponentInfo> findByScope(Class scope) {
        if (!Annotation.class.isAssignableFrom(scope)) {
            return null;
        }
        return scopedInfo.get(scope);
    }

    @Override
    public List<ComponentInfo> findAll() {
        return scopedInfo.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void addListener(AfterRegisterListener listener) {
        if (!afterRegisterListeners.contains(listener)) {
            afterRegisterListeners.add(listener);
        }
    }

    @Override
    public void removeListener(AfterRegisterListener listener) {
        afterRegisterListeners.remove(listener);
    }

    @Override
    public List<AfterRegisterListener> getAllListeners() {
        return afterRegisterListeners.stream()
                .collect(Collectors.toUnmodifiableList());
    }
}
