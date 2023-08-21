package org.swdc.dependency.registry;


import org.swdc.dependency.interceptor.AspectAt;
import org.swdc.dependency.utils.AnnotationUtil;
import org.swdc.ours.common.annotations.Annotations;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Pattern;

public class InterceptorInfo {

    private String namePattern;

    private Class[] returnType;

    private Class annotationType;

    private AspectAt at;

    private Method method;

    private int order = 0;

    public void setMethod(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public AspectAt getAt() {
        return at;
    }

    public Class getAnnotationType() {
        return annotationType;
    }

    public Class[] getReturnType() {
        return returnType;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public void setReturnType(Class[] returnType) {
        this.returnType = returnType;
    }

    public void setAnnotationType(Class annotationType) {
        this.annotationType = annotationType;
    }

    public void setAt(AspectAt at) {
        this.at = at;
    }

    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    public boolean match(Method method) {
        if (this.getReturnType() != null) {
            Class returnType = method.getReturnType();
            return Arrays.asList(this.returnType).contains(returnType);
        }
        if (this.getAnnotationType() != null) {
            return Annotations.findAnnotation(method,this.getAnnotationType()) != null;
        }
        if (this.getNamePattern() != null) {
            return Pattern.compile(this.namePattern).matcher(method.getName()).find();
        }
        return false;
    }

}
