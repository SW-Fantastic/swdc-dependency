package org.swdc.dependency;

/**
 * 组件的Scope的接口。
 *
 * 使用此接口可以实现新的Scope。
 * Scope的使用方法：
 * 将新的Scope注解定义中加入Scope注解（javax），
 * 然后加入ScopeImplement注解（本工程），并且ScopeImplement注解
 * 指向实现此接口的类，实现类需要一个无参数构造方法。
 *
 * 如果必须有参数，则需要通过DependencyEnvironment手动添加和使用。
 *
 */
public interface DependencyScope extends DependencyContext {

    /**
     * Scope类型
     * @return
     */
    Class getScopeType();

    /**
     * 添加多实现的组件对象
     * @param name 组件名
     * @param clazz 组件类型
     * @param multiple 是那个接口（抽象类的）多实现
     * @param component 组件对象
     * @param <T> 类型
     * @return 添加的组件
     */
    <T> T put(String name, Class clazz, Class multiple, T component);

    /**
     * 添加组件
     * @param name 组件名
     * @param clazz 组件类型
     * @param component 组件对象
     * @param <T> 组件类型
     * @return 添加的组件
     */
    <T> T put(String name, Class clazz, T component);

}
