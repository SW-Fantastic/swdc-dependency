package org.swdc.dependency.registry;

import org.swdc.dependency.registry.ComponentInfo;
import org.swdc.dependency.registry.DependencyRegister;

import java.util.List;

/**
 * 组件注册空间接口
 */
public interface DependencyRegisterContext extends DependencyRegister {

    /**
     * 根据类型查找组件信息
     * @param clazz 类型
     * @return
     */
    ComponentInfo findByClass(Class clazz);

    /**
     * 根据组件名查找组件信息
     * @param named
     * @return
     */
    ComponentInfo findByNamed(String named);

    /**
     * 查找抽象组件的所有实现
     * @param clazz 抽象类或接口
     * @return
     */
    List<ComponentInfo> findByAbstract(Class clazz);

    /**
     * 查找某一个scope里面的所有组件
     * @param scope
     * @return
     */
    List<ComponentInfo> findByScope(Class scope);

    /**
     * 查找现有的所有组件信息
     * @return
     */
    List<ComponentInfo> findAll();

}
