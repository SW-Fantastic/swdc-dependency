package org.swdc.dependency.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TypedKey<T> {

    private static Map<Class, Map<String,TypedKey>> keys = new ConcurrentHashMap<>();

    private Class<T> type;
    private String name;

    private TypedKey(Class<T> type,String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        TypedKey<?> typedKey = (TypedKey<?>) o;
        return Objects.equals(type, typedKey.type) && Objects.equals(name, typedKey.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    public static <T> TypedKey<T> getTypedKey(Class<T> type, String name) {
        if (ReflectionUtil.isBoxedType(type)) {
            type = ReflectionUtil.getBasicType(type);
        }
        if (keys.containsKey(type)) {
            Map<String,TypedKey> typedKeyMap = keys.get(type);
            if (!typedKeyMap.containsKey(name)) {
                TypedKey key = new TypedKey(type,name);
                typedKeyMap.put(name,key);
                return key;
            }
            return typedKeyMap.get(name);
        }
        TypedKey<T> key = new TypedKey<>(type,name);
        Map<String,TypedKey> keyMap = keys.getOrDefault(type,new HashMap<>());
        keyMap.put(name,key);
        keys.put(type,keyMap);
        return key;
    }

}
