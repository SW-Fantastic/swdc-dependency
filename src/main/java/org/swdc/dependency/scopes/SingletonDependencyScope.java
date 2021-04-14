package org.swdc.dependency.scopes;

import jakarta.inject.Singleton;
import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.DependencyScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SingletonDependencyScope implements DependencyScope {

    private DependencyContext context;

    private Map<String,Object> namedComponents = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Class, List<Object>> typedComponents = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Class, List<Object>> multipleComponents = new ConcurrentHashMap<>();

    @Override
    public Class getScopeType() {
        return Singleton.class;
    }

    @Override
    public <T> T put(String name, Class clazz, Class multiple, T component) {

        component = this.put(name,clazz,component);

        if (multiple != null) {
            List<Object> list = multipleComponents.getOrDefault(multiple,new ArrayList<>());
            list.add(component);
            multipleComponents.put(multiple,list);
        }

        return component;
    }

    @Override
    public <T> T put(String name, Class clazz, T component) {
        List<Object> list = typedComponents.getOrDefault(clazz,new ArrayList<>());
        list.add(component);
        typedComponents.put(clazz,list);

        if (!name.equals(clazz.getName())) {
            namedComponents.put(name,component);
        }
        return component;
    }

    @Override
    public <T> T getByClass(Class<T> clazz) {
        List<Object> typed = typedComponents.get(clazz);
        if (typed == null || typed.size() == 0) {
            return null;
        }
        if (typed.size() > 1) {
            throw new IllegalStateException("存在多个组件，请使用命名组件的方式处理。\n" +
                    "there are multiple components， please using named annotation。" + clazz.getName());
        }
        return (T)typed.get(0);
    }

    @Override
    public <T> T getByName(String name) {
        return (T)namedComponents.get(name);
    }

    @Override
    public <T> List<T> getByAbstract(Class<T> parent) {
        return (List<T>)multipleComponents.get(parent);
    }

    @Override
    public List<Object> getAllComponent() {
        return typedComponents.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void setContext(DependencyContext context) {
        this.context = context;
    }
}
