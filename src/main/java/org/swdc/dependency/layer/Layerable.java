package org.swdc.dependency.layer;

import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.registry.ComponentInfo;

import java.util.List;

public interface Layerable extends DependencyContext {

    /**
     * 获取本层所有导出的组件
     * @return
     */
    List<ComponentInfo> getExport();

    /**
     * 本层是否包含此类型
     * （用于判断应用是否应该主动创建此组件）
     * @param clazz
     * @return
     */
    boolean contains(Class clazz);

    /**
     * 从哪一个层导入依赖
     * @param layer
     */
    void setImport(Layer layer);

    /**
     * 按name导出组件给其他层
     * @param name
     */
    void exportByName(String name);

    /**
     * 按class导出组件给其他层
     * @param clazz
     */
    void exportByClass(Class clazz);

    /**
     * 按抽象类和接口导出组件给其它层
     * @param clazz
     */
    void exportByAbstract(Class clazz);

}
