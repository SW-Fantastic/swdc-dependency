package org.swdc.dependency.scopes;

import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.registry.ComponentInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组件的临时缓存池。
 *
 * 未完成创建的组件会临时性存放的位置。
 * 用来检查和处理循环依赖。
 *
 */
public class CacheDependencyHolder implements DependencyContext {

    private Map<Class, List<Object>> typedCache = new HashMap<>();
    private Map<Class, List<Object>> multipleCache = new HashMap<>();
    private Map<String, Object> namedCache = new HashMap<>();
    private List<Class> creating = new ArrayList<>();

    public void begin(Class clazz) {
        creating.add(clazz);
    }

    public boolean isCreating(Class clazz) {
        return creating.contains(clazz);
    }

    public <T> T put(ComponentInfo info, T unResolved) {
        creating.remove(info.getClazz());

        List<Object> instanceList = typedCache.getOrDefault(info.getClazz(),new ArrayList<>());
        instanceList.add(unResolved);
        typedCache.put(info.getClazz(),instanceList);

        if (info.getName() != null) {
            namedCache.put(info.getName(),unResolved);
        }

        if (info.getAbstractClazz() != null) {
            instanceList = multipleCache.getOrDefault(info.getClazz(),new ArrayList<>());
            instanceList.add(unResolved);
            typedCache.put(info.getClazz(),instanceList);
        }

        return unResolved;
    }

    public void complete(ComponentInfo info) {
        if (isCreating(info.getClazz())) {
            throw new RuntimeException("组件尚未创建完成。");
        }
        this.typedCache.remove(info.getClazz());
        if (!info.getName().equals(info.getClazz().getName())) {
            namedCache.remove(info.getName());
        }
        if (info.getAbstractClazz() != null) {
            multipleCache.remove(info.getClazz());
        }
    }

    @Override
    public Object getByClass(Class clazz) {
        List<Object> instances = typedCache.get(clazz);
        if (instances == null || instances.size() > 1) {
            return null;
        }
        return instances.get(0);
    }

    @Override
    public Object getByName(String name) {
        return namedCache.get(name);
    }

    @Override
    public List<Object> getByAbstract(Class parent) {
        return multipleCache.get(parent);
    }

    @Override
    public List<Object> getAllComponent() {
        return  typedCache.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
