package org.swdc.dependency.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtil {

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

}
