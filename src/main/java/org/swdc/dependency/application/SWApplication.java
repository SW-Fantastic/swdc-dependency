package org.swdc.dependency.application;

import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.EnvironmentLoader;
import org.swdc.dependency.layer.Layer;

public interface SWApplication {

    /**
     * 加载config的阶段
     * 允许用户加载自定义的配置
     * @param loader
     */
    void onConfig(EnvironmentLoader loader);

    /**
     * 启动阶段，启动加载后允许用户手动加载
     * 自己的layer
     * @param layer
     */
    void onLaunch(Layer layer);

    /**
     * 依赖环境处理完毕，可以开始应用的具体内容了
     * @param context
     */
    void onStarted(DependencyContext context);

    /**
     * 应用结束，关闭环境。
     * @param context
     */
    void onShutdown(DependencyContext context);

}
