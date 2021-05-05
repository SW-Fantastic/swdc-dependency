package org.swdc.dependency.layer;

import org.swdc.dependency.DependencyContext;

public interface LayerLoader {

    /**
     * 指定module，一般是指向加载他的Application
     * 这样层就可以通过module加载Application的配置。
     * @param module
     */
    void setEnvironmentModule(Module module);

    /**
     * 加载此依赖环境层。
     * @return
     */
    DependencyContext load();

}
