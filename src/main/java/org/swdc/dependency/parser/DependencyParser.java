package org.swdc.dependency.parser;

import org.swdc.dependency.registry.DependencyRegisterContext;
import org.swdc.dependency.listeners.AfterRegisterListener;
import org.swdc.dependency.registry.ComponentInfo;

import java.util.Collection;

public interface DependencyParser<S> {

    /**
     * 解析组件的方法
     * @param source 组件的描述符
     * @param context 组件注册空间的上下文
     */
    void parse(S source, DependencyRegisterContext context);

    /**
     * 解析后引发相关的listener
     * @param info
     * @param listeners
     * @return
     */
    default ComponentInfo invokeListeners(ComponentInfo info,Collection<AfterRegisterListener> listeners) {
        for (AfterRegisterListener listener: listeners) {
            info = listener.afterCreated(info);
        }
        return info;
    }

}
