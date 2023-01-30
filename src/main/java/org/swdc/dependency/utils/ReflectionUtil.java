package org.swdc.dependency.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReflectionUtil {

    public static List<Method> findAllMethods(Class clazz) {
        List<Method> methodList = new ArrayList<>();
        Class current = clazz;

        while (current != null) {
            if (current == Object.class) {
                break;
            }
            Method[] methods = current.getMethods();
            for (Method method: methods) {
                methodList.add(method);
            }
            current = current.getSuperclass();
        }
        return methodList;
    }

    /**
     * 获取Class的可注入方法
     * @param clazz 类
     * @return 方法列表
     */
    public static List<Method> findDependencyMethods(Class clazz) {
        List<Method> methodList = new ArrayList<>();
        Class current = clazz;

        while (current != null) {
            Method[] methods = current.getMethods();
            for (Method method: methods) {
                if (AnnotationUtil.hasDependency(method)) {
                    methodList.add(method);
                }
            }
            current = current.getSuperclass();
        }
        return methodList;
    }

    public static List<Field> findFieldsByAnnotation(Class clazz,Class annotatedWith) {
        List<Field> fieldList = new ArrayList<>();
        Class current = clazz;

        while (current != null) {
            Field[] fields = current.getDeclaredFields();
            for (Field field: fields) {
                AnnotationDescription desc = AnnotationUtil.findAnnotation(field,annotatedWith);
                if (desc != null) {
                    fieldList.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fieldList;
    }


    /**
     * 获取类的可注入字段
     * @param clazz 类
     * @return 字段列表
     */
    public static List<Field> findDependencyFields(Class clazz) {
        List<Field> methodList = new ArrayList<>();
        Class current = clazz;
        while (current != null) {
            Field[] fields = current.getDeclaredFields();
            for (Field field: fields) {
                if (AnnotationUtil.hasDependency(field)) {
                    methodList.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return methodList;
    }

    public static boolean isBasicType(Class type) {
        if (type == int.class ||
                type == float.class ||
                type == double.class ||
                type == char.class ||
                type == byte.class ||
                type == short.class) {
            return  true;
        }
        return  false;
    }

    public static boolean isBoxedType(Class type) {
        if (type == Integer.class ||
                type == Float.class ||
                type == Double.class ||
                type == Character.class ||
                type == Byte.class ||
                type == Boolean.class||
                type == Short.class) {
            return  true;
        }
        return  false;
    }

    public static Class getBasicType(Class type){
        if (isBoxedType(type)) {
            if (Integer.class.equals(type)) {
                return int.class;
            } else if (Double.class.equals(type)) {
                return double.class;
            } else if (Float.class.equals(type)) {
                return float.class;
            } else if (Character.class.equals(type)) {
                return char.class;
            } else if (Byte.class.equals(type)) {
                return byte.class;
            } else if (Boolean.class.equals(type)){
                return boolean.class;
            } else if (Short.class.equals(type)) {
                return short.class;
            }
        } else if (isBasicType(type)) {
            return type;
        }
        throw new RuntimeException(type.getName() + "不是一个包装类型，无法进行转换");
    }

}
