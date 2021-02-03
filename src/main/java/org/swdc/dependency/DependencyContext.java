package org.swdc.dependency;

import java.util.List;

/**
 * 上下文接口。
 * 用于多个位置，只要需要根据某些条件获取组件
 * 就都可以使用此接口
 */
public interface DependencyContext {

    /**
     * 根据类获取组件。
     *
     * 组件还没有解析和创建的话，本方法应该创建
     * 此组件。
     *
     * @param clazz 类型（不可以是抽象类，接口）
     * @param <T> 返回的组件类型
     * @return 组件（找不到可能是空）
     */
    <T> T getByClass(Class<T> clazz);

    /**
     * 根据名称获取组件.
     *
     * 组件如果已经解析但是没有创建的话，应当创建。
     * 如果组件没有解析，则返回空
     *
     * @param name 组件名
     * @param <T> 组件类型
     * @return 组件
     */
    <T> T getByName(String name);

    /**
     * 获取多实现的组件。
     * @param parent 被实现的接口
     * @param <T> 接口类型
     * @return 所有实现的组件
     */
    <T>  List<T> getByAbstract(Class<T> parent);

    List<Object> getAllComponent();

}
