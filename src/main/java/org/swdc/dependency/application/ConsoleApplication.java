package org.swdc.dependency.application;

import org.swdc.dependency.AnnotationLoader;
import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.EnvironmentLoader;
import org.swdc.dependency.layer.Layer;
import org.swdc.dependency.layer.LayerLoader;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

public abstract class ConsoleApplication implements SWApplication {

    private List<String> args;
    private DependencyContext context;

    public List<String> getArgs() {
        return args;
    }

    public void launch(String[] args) {
        this.args = Arrays.asList(args);
        // 环境加载器
        AnnotationLoader loader = new AnnotationLoader();
        // 拓展点：允许在这里手动配置一些东西。
        this.onConfig(loader);

        DependencyContext context = loader.load();
        Layer layer = new Layer(context);

        // 拓展点：Launch，可以在这里手动添加环境层，
        // 如果需要的层对顺序比较敏感，就可以在这里处理。
        this.onLaunch(layer);

        // 加载其他的环境层
        ServiceLoader<LayerLoader> layerLoaders = ServiceLoader.load(LayerLoader.class);
        for (LayerLoader layerLoader: layerLoaders) {
            layerLoader.setEnvironmentModule(this.getClass().getModule());
            DependencyContext ctx = layerLoader.load();
            layer.based(ctx);
        }
        this.context = layer.asContext();
        // 依赖环境处理完毕，启动应用并挂载关闭钩子。
        this.onStarted(context);
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
    }

    private void onShutdown() {
        this.onShutdown(context);
        if (context instanceof Closeable) {
            try {
                Closeable closeable = (Closeable) context;
                closeable.close();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onConfig(EnvironmentLoader loader) {

    }

    @Override
    public void onLaunch(Layer layer) {

    }
}
