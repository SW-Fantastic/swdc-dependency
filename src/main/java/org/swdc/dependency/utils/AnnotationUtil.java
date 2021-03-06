package org.swdc.dependency.utils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.swdc.dependency.annotations.AliasFor;
import org.swdc.dependency.annotations.Aware;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationUtil {

    public static boolean hasDependency(AnnotatedElement element) {
        Map<Class,AnnotationDescription> descriptions = getAnnotations(element);
        List<Class> injectable = List.of(Inject.class,Resource.class,Aware.class,Named.class);
        return injectable
                .stream()
                .anyMatch(c -> findAnnotationIn(descriptions,c) != null);
    }

    /**
     * 解析某个反射对象的注解
     * @param element 可被注解的对象
     * @return 注解描述列表
     */
    public static Map<Class,AnnotationDescription> getAnnotations(AnnotatedElement element) {
        Map<Class,AnnotationDescription> desc = new HashMap<>();
        Annotation[] annotations = element.getAnnotations();
        for (Annotation annotation: annotations) {
            Class clazz = annotation.annotationType();
            if (clazz == Target.class ||clazz == Retention.class || clazz == Documented.class) {
                continue;
            }

            Map<Class,AnnotationDescription> parent = getAnnotations(clazz);

            AnnotationDescription annoDesc = new AnnotationDescription(clazz);
            List<Method> methods = getAnnotationMethods(clazz);

            for (AnnotationDescription description: parent.values()) {
                description.setDeclareOn(annoDesc);
            }

            for (Method method: methods) {
                try {
                    Object val = method.invoke(annotation);
                    Class propType = val.getClass();
                    String propName = method.getName();
                    annoDesc.putProperty(propType,propName,val);
                    AliasFor aliasFor = method.getAnnotation(AliasFor.class);
                    if (aliasFor != null) {
                        AnnotationDescription parentDesc = parent.get(aliasFor.annotation());
                        parentDesc.putProperty(propType,aliasFor.value(),val);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            annoDesc.setAnnotations(parent);
            desc.put(clazz,annoDesc);
        }
        return desc;
    }

    public static AnnotationDescription findAnnotationIn(Map<Class,AnnotationDescription> map, Class annotation) {
        if (map.containsKey(annotation)) {
            return map.get(annotation);
        }
        for (AnnotationDescription desc: map.values()) {
            AnnotationDescription found = desc.find(annotation);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static AnnotationDescription findAnnotation(AnnotatedElement elem, Class annotationType) {
        Map<Class,AnnotationDescription> descriptionMap = getAnnotations(elem);
        return findAnnotationIn(descriptionMap,annotationType);
    }

    public static List<Method> getAnnotationMethods(Class annotationType) {
        List<String> methodNames = Arrays.asList("toString","equals","annotationType","hashCode");

        return Stream.of(annotationType.getMethods())
                .filter(m -> m.getParameters().length == 0)
                .filter(m -> !methodNames.contains(m.getName()))
                .collect(Collectors.toList());
    }

}
