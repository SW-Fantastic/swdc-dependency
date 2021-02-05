package org.swdc.dependency.registry;

import jakarta.inject.Provider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ComponentInfo {

    /**
     * 被描述的类
     */
    private Class clazz;

    /**
     * 组件名（使用Named定义）
     */
    private String name;

    /**
     * 作用域
     */
    private Class scope;

    /**
     * 是否为对某类（接口）的多实现
     */
    private boolean multiple;

    /**
     * 多实现的父类（接口）是什么
     */
    private Class abstractClazz;

    /**
     * 是否已经注册
     */
    private boolean registered;

    /**
     * 是否处理完成
     */
    private boolean resolved;

    /**
     * 动态工厂
     */
    private Class factory;

    /**
     * 静态工厂
     */
    private Method factoryMethod;

    /**
     * 构造方法的注入点
     */
    private ConstructorInfo constructorInfo;

    /**
     * 工厂方法注入点
     */
    private FactoryDependencyInfo factoryInfo;

    /**
     * 普通注入点
     */
    private List<DependencyInfo> dependencyInfos = new ArrayList<>();

    /**
     * 初始化方法
     */
    private Method initMethod;

    /**
     * 销毁方法
     */
    private Method destroyMethod;

    public ComponentInfo(Class clazz, String name, Class scope) {
        this.clazz = clazz;
        this.scope = scope;
        this.name = name;
    }

    public ComponentInfo(Class abstractClazz,Class clazz, String name,Class scope) {
        this(clazz,name,scope);
        this.multiple = true;
        this.abstractClazz = abstractClazz;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public ConstructorInfo getConstructorInfo() {
        return constructorInfo;
    }

    public void setConstructorInfo(ConstructorInfo constructorInfo) {
        this.constructorInfo = constructorInfo;
    }

    public List<DependencyInfo> getDependencyInfos() {
        return dependencyInfos;
    }

    public Class getScope() {
        return scope;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public Class getAbstractClazz() {
        return abstractClazz;
    }

    public String getName() {
        return name;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setFactory(Class factory) {
        this.factory = factory;
    }

    public void setFactoryMethod(Method factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    public Class getFactory() {
        return factory;
    }

    public Method getFactoryMethod() {
        return factoryMethod;
    }

    public void setFactoryInfo(FactoryDependencyInfo factoryInfo) {
        this.factoryInfo = factoryInfo;
    }

    public FactoryDependencyInfo getFactoryInfo() {
        return factoryInfo;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public Method getDestroyMethod() {
        return destroyMethod;
    }

    public Method getInitMethod() {
        return initMethod;
    }

    public void setInitMethod(Method initMethod) {
        this.initMethod = initMethod;
    }

    public void setDestroyMethod(Method destroyMethod) {
        this.destroyMethod = destroyMethod;
    }

    public boolean isFactoryComponent() {
        return Provider.class.isAssignableFrom(clazz);
    }

}
