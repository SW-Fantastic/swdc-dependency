package org.swdc.dependency.application;

import org.swdc.dependency.AnnotationLoader;
import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.EnvironmentLoader;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractApplication implements SWApplication {

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


}
