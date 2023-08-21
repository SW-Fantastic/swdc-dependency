package org.swdc.dependency.utils;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.swdc.dependency.annotations.Aware;
import org.swdc.ours.common.annotations.AnnotationDescriptions;
import org.swdc.ours.common.annotations.Annotations;

import java.lang.reflect.AnnotatedElement;
import java.util.*;

public class AnnotationUtil {

    public static boolean hasDependency(AnnotatedElement element) {
        AnnotationDescriptions descriptions = Annotations.getAnnotations(element);
        List<Class> injectable = List.of(Inject.class,Resource.class,Aware.class,Named.class);
        return injectable
                .stream()
                .anyMatch(c -> Annotations.findAnnotationIn(descriptions,c) != null);
    }


}
