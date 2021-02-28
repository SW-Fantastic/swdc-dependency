package org.swdc.dependency;

import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.listeners.AfterRegisterListener;

import java.util.Collection;

public interface DependencyEnvironment extends DependencyContext, AutoCloseable {

    /**
     * 获取注册的容器中的Scope
     * @return
     */
    Collection<DependencyScope> getScopes();

    /**
     * 获取此类型的Scope
     * @param scope
     * @return
     */
    DependencyScope getScope(Class scope);

    /**
     * 向容器添加一个Scope
     * @param scope
     */
    void registerScope(DependencyScope scope);

    /**
     * 组件注册，解析组件的信息
     * @param component
     */
    void registerComponent(Class component);

    /**
     * 实例注册，直接向容器添加组件和组件对应的实例
     * 对象，一般用于添加不方便容器直接创建的对象，例如
     * Configure。
     *
     * @param component
     * @param instance
     */
    <T> void registerInstance(Class<T> component, T instance);

    /**
     * 组件创建监听，当组件被实例化并且初始化后，进入容器前
     * 触发。
     * @param listener
     */
    void registerCreationListener(AfterCreationListener listener);

    /**
     * 组件解析监听，在组件的信息被解析后会触发。
     * @param registerListener
     */
    void registerParsedListener(AfterRegisterListener registerListener);

}
