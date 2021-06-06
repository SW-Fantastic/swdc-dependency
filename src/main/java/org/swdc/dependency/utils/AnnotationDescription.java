package org.swdc.dependency.utils;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * 注解描述器
 * 用来描述一个注解。
 */
public class AnnotationDescription {

    /**
     * 此对象描述的注解的类型
     */
    private Class annotation;

    /**
     * 此注解对象包含的父注解和描述对象
     */
    private Map<Class,AnnotationDescription> annotations;

    /**
     * 此注解的父注解对象
     */
    private AnnotationDescription declareOn;

    /**
     * 此注解含有的属性
     */
    private Map<TypedKey,Object> properties = new HashMap<>();

    public AnnotationDescription(Class annotation) {
        this.annotation = annotation;
    }

    <T> void putProperty(Class<T> type, String property, T val) {
        TypedKey<T> key = TypedKey.getTypedKey(type,property);
        properties.put(key,val);
    }

    public <T> T getProperty(Class<T> type, String name) {
        TypedKey<T> key = TypedKey.getTypedKey(type,name);
        return (T)properties.get(key);
    }

    void setAnnotations(Map<Class,AnnotationDescription> annotations) {
        this.annotations = annotations;
    }

    public Map<Class, AnnotationDescription> getAnnotations() {
        return annotations;
    }

    public AnnotationDescription find(Class annotation) {
        if (annotations.containsKey(annotation)) {
            return annotations.get(annotation);
        }
        for (AnnotationDescription annotationDescription: annotations.values()) {
            AnnotationDescription desc = annotationDescription.find(annotation);
            if(desc != null) {
                return desc;
            }
        }
        return null;
    }

    void setDeclareOn(AnnotationDescription desc) {
        this.declareOn = desc;
    }

    public AnnotationDescription getDeclareOn() {
        return declareOn;
    }

    public Class getAnnotation() {
        return annotation;
    }
}
