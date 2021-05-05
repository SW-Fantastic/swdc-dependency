package org.swdc.dependency.registry;

import org.swdc.dependency.registry.ComponentInfo;

/**
 * 依赖注册接口。
 *
 * 注册和链接依赖信息。
 */
public interface DependencyRegister {

    /**
     * 注册一个组件
     * @param info 组件描述符
     * @return
     */
    ComponentInfo register(ComponentInfo info);

}
