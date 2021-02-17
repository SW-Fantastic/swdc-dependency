package org.swdc.dependency;

import org.swdc.dependency.registry.ComponentInfo;

public interface DependencyFactory extends DependencyContext {

    /**
     * 根据组件信息创建组件
     * @param info 组件信息
     * @param <T> 组件类型
     * @return 未初始化的组件对象
     */
    <T> T create(ComponentInfo info);

    /**
     * 初始化组件
     * @param info 组件信息
     * @param object 组件对象
     * @param <T> 组件类型
     * @return 初始化后的组件
     */
    <T> T initialize(ComponentInfo info, T object);

    /**
     * 获取组件，包括创建中ed
     * @param info 组件信息
     * @param <T> 组件类
     * @return 组件
     */
    <T> T getInternal(ComponentInfo info,Object other);

    /**
     * 获取工厂组件。
     * @param clazz 工厂类
     * @param <T> 组件类型
     * @return 工厂组件的对象
     */
    <T> T getFactory(Class clazz);

    /**
     * 获取AOP的增强对象
     * @param clazz
     * @param <T>
     * @return
     */
    <T> T getInterceptor(Class<T> clazz);

}
