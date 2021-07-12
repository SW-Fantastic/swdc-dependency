package org.swdc.dependency;

import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.listeners.AfterRegisterListener;

public interface EnvironmentLoader<T extends DependencyContext> {

    /**
     * 为依赖环境添加provider提供一个组件
     * @param provider provider
     * @return
     */
    EnvironmentLoader<T> withProvider(Class provider);

    /**
     * 通过组件的配置类注册组件
     * @param declare
     * @return
     */
    EnvironmentLoader<T> withDeclare(Class declare);

    /**
     * 注册新的scope
     * @param scope
     * @return
     */
    EnvironmentLoader<T> withScope(DependencyScope scope);

    /**
     *手动注册组件
     * @param component
     * @return
     */
    EnvironmentLoader<T> withComponent(Class component);

    /**
     * 注册手动初始化后的组件
     * @param clazz 组件类型
     * @param instance 组件实例
     * @return
     */
    <C> EnvironmentLoader<T> withInstance(Class<C> clazz, C instance);

    /**
     * 包扫描注册组件（不建议使用）
     * @param packageName 包名
     * @return
     */
    EnvironmentLoader<T> withPackage(String packageName);



    /**
     * 监听器注册
     * @param listener
     * @return
     */
    EnvironmentLoader<T> afterRegister(AfterRegisterListener listener);

    /**
     * 监听器注册
     * @param listener
     * @return
     */
    EnvironmentLoader<T> afterCreated(AfterCreationListener listener);

    /**
     * 按照上述配置创建依赖环境
     * @return
     */
    DependencyContext load();

}
