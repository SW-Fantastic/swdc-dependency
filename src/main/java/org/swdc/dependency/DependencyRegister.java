package org.swdc.dependency;

import org.swdc.dependency.registry.ComponentInfo;

/**
 * 依赖注册接口。
 *
 * 注册和链接依赖信息。
 */
public interface DependencyRegister {

    ComponentInfo register(ComponentInfo info);

}
