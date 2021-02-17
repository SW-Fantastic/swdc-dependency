package org.swdc.dependency.registry;

import jakarta.inject.Provider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 组件描述符
 *
 * 用来说明一个组件应该怎么创建，有哪些依赖，
 * 是不是工厂和切面等特殊组件之类的。
 */
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

    /**
     * 是否是一个切面
     */
    private boolean interceptor;

    /**
     * 如果本组件是切面，这里是切面方法的描述
     */
    private List<InterceptorInfo> interceptorInfos = new ArrayList<>();

    /**
     * 如果本组件含有With注解，被切面AOP，
     * 这里应该包含切面的组件数据
     */
    private List<ComponentInfo> adviceBy = new ArrayList<>();

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

    /**
     * 给当前的组件添加一个切面
     * @param info 切面的组件数据
     */
    public void addAdviceBy(ComponentInfo info) {
        if (!info.isInterceptor()) {
            return;
        }
        adviceBy.add(info);
    }

    /**
     * 当前组件是一个切面，为切面组件添加切面的方法描述。
     * @param interceptorInfo 切面方法的描述
     */
    public void addInterceptorInfo(InterceptorInfo interceptorInfo) {
        this.interceptorInfos.add(interceptorInfo);
    }

    // -------------------------Getter 和 Setter---------------------------------

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

    public boolean isInterceptor() {
        return interceptor;
    }

    public void setInterceptor(boolean interceptor) {
        this.interceptor = interceptor;
    }

    public List<InterceptorInfo> getInterceptorInfos() {
        return interceptorInfos;
    }

    public List<ComponentInfo> getAdviceBy() {
        return adviceBy;
    }

}
