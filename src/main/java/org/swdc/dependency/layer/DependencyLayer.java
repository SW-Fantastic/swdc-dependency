package org.swdc.dependency.layer;

import java.util.List;
/**
 * 依赖层叠接口。
 * 多个Environment将会通过此接口层层叠加和组合，
 * environment内部提供组件的info并且独立维护组件。
 *
 * 通过责任链向parent查询和获取组件，如果没有，父层级
 * 不负责进行组件解析。
 *
 * 这可以允许我们通过environment封装一组特定的功能。
 */
public interface DependencyLayer  {

    /**
     *
     * 按照Class查找组件
     *
     * 首先查找本层的export，一般第一层是开放层，所以
     * 直接向上层查找，上层也会首先查询export，但是除第一层外
     * 都是封闭层，封闭层不能解析组件，也就是说如果Parent层都没有，
     * 就会由第一层（开放层）解析和创建。
     *
     * @param clazz 组件类
     * @param <T>
     * @return
     */
    <T> T findParentByClass(Class<T> clazz);

    /**
     *
     * 按照组件名查找组件
     *
     * 首先查找本层的export，一般第一层是开放层，所以
     * 直接向上层查找，上层也会首先查询export，但是除第一层外
     * 都是封闭层，封闭层不能解析组件，也就是说如果Parent层都没有，
     * 就会由第一层（开放层）解析和创建。
     *
     * @param name 组件名
     * @param <T>
     * @return
     */
    <T> T findParentByName(String name);

    /**
     *
     * 按照Class查找组件工厂
     *
     * 组件工厂不对外开放，只用来创建组件，不能在getByClass和getByName之类的
     * 获取到。
     *
     * 首先查找本层的export，一般第一层是开放层，所以
     * 直接向上层查找，上层也会首先查询export，但是除第一层外
     * 都是封闭层，封闭层不能解析组件，也就是说如果Parent层都没有，
     * 就会由第一层（开放层）解析和创建。
     *
     * @param clazz 组件类
     * @param <T>
     * @return
     */
    <T> T findParentFactory(Class clazz);

    /**
     *
     * 按照Class查找AOP增强组件
     *
     * 首先查找本层的export，一般第一层是开放层，所以
     * 直接向上层查找，上层也会首先查询export，但是除第一层外
     * 都是封闭层，封闭层不能解析组件，也就是说如果Parent层都没有，
     * 就会由第一层（开放层）解析和创建。
     *
     * 组件增强不对外开放，不能在getByClass和getByName之类的
     * 获取到。
     *
     * @param clazz 组件类
     * @param <T>
     * @return
     */
    <T> T findParentInterceptor(Class clazz);

    /**
     *
     * 按照Class查找抽象
     *
     * 首先查找本层的export，一般第一层是开放层，所以
     * 直接向上层查找，上层也会首先查询export，但是除第一层外
     * 都是封闭层，封闭层不能解析组件，也就是说如果Parent层都没有，
     * 就会由第一层（开放层）解析和创建。
     *
     * @param clazz 组件抽象类
     * @param <T>
     * @return
     */
    <T> List<T> findParentByAbstract(Class<T> clazz);

    /**
     * 获取上层
     * @return
     */
    Layer getParent();

}
