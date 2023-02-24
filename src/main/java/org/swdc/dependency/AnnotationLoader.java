package org.swdc.dependency;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.inject.Scope;
import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.listeners.AfterRegisterListener;
import org.swdc.dependency.registry.ComponentInfo;
import org.swdc.dependency.registry.DependencyRegisterContext;
import org.swdc.dependency.utils.AnnotationUtil;

import java.util.List;

/**
 * 基于注解的依赖环境加载器。
 */
public class AnnotationLoader implements EnvironmentLoader<AnnotationEnvironment> {

    private AnnotationEnvironment annotationEnvironment;
    private boolean closed;

    public AnnotationLoader() {
        annotationEnvironment = new AnnotationEnvironment();
    }

    @Override
    public AnnotationLoader withProvider(Class provider) {
        if (closed) {
            return null;
        }
        annotationEnvironment.registerComponent(provider);
        return this;
    }

    @Override
    public AnnotationLoader withDeclare(Class declare) {
        if (closed) {
            return null;
        }
        annotationEnvironment.registerComponent(declare);
        return this;
    }

    @Override
    public AnnotationLoader withScope(DependencyScope scope) {
        if (closed) {
            return null;
        }
        annotationEnvironment.registerScope(scope);
        return this;
    }

    @Override
    public AnnotationLoader withComponent(Class component) {
        if (closed) {
            return null;
        }
        annotationEnvironment.registerComponent(component);
        return this;
    }

    @Override
    public <C> EnvironmentLoader<AnnotationEnvironment> withInstance(Class<C> clazz, C instance) {
        if (closed) {
            return null;
        }
        annotationEnvironment.registerInstance(clazz,instance);
        return this;
    }

    @Override
    public AnnotationLoader withPackage(String packageName) {
        ClassGraph graph = new ClassGraph();
        ScanResult result = graph.enableAllInfo()
                .acceptPackages(packageName)
                .scan();
        List<Class<?>> classList = result.getAllStandardClasses().loadClasses();
        for (Class clazz: classList){
            this.annotationEnvironment.registerComponent(clazz);
        }
        return this;
    }


    @Override
    public AnnotationLoader afterRegister(AfterRegisterListener listener) {
        if (closed) {
            return null;
        }
        annotationEnvironment.registerParsedListener(listener);
        return this;
    }

    @Override
    public AnnotationLoader afterCreated(AfterCreationListener listener) {
        if (closed) {
            return null;
        }
        annotationEnvironment.registerCreationListener(listener);
        return this;
    }

    @Override
    public DependencyContext load() {
        if (closed) {
            return null;
        }
        closed = true;
        DependencyContext context = annotationEnvironment;
        return context;
    }
}
