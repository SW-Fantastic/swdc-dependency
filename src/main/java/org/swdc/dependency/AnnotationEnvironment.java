package org.swdc.dependency;

import jakarta.annotation.Resource;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.swdc.dependency.annotations.Dependency;
import org.swdc.dependency.annotations.ScopeImplement;
import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.listeners.AfterRegisterListener;
import org.swdc.dependency.parser.AnnotationDependencyParser;
import org.swdc.dependency.registry.*;
import org.swdc.dependency.scopes.SingletonDependencyScope;
import org.swdc.dependency.utils.AnnotationDescription;
import org.swdc.dependency.utils.AnnotationUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AnnotationEnvironment extends EnvironmentFactory implements DependencyEnvironment,Listenable<AfterCreationListener> {

    private Map<Class, DependencyScope> scopes;

    private DefaultDependencyRegistryContext registryContext;
    private DependencyParser<Class> parser;

    private Map<Class, Object> factoryMap;

    private List<AfterCreationListener> afterCreationListeners;

    private AtomicBoolean closed = new AtomicBoolean(false);

    public AnnotationEnvironment() {
        registryContext = new DefaultDependencyRegistryContext();
        parser = new AnnotationDependencyParser();
        scopes = new ConcurrentHashMap<>();
        afterCreationListeners = new ArrayList<>();
        factoryMap = new HashMap<>();

        scopes.put(Singleton.class,new SingletonDependencyScope());
    }

    @Override
    public Collection<DependencyScope> getScopes() {
        return scopes.values();
    }

    @Override
    public void registerScope(DependencyScope scope) {
        checkStatus();
        scopes.put(scope.getScopeType(),scope);
    }

    @Override
    public void registerComponent(Class component) {
        checkStatus();
        ComponentInfo info = this.registryContext.findByClass(component);
        if (info == null) {
            parser.parse(component,this.registryContext);
        }
    }

    @Override
    public void registerCreationListener(AfterCreationListener listener) {
        checkStatus();
        this.addListener(listener);
    }

    @Override
    public void registerParsedListener(AfterRegisterListener registerListener) {
        checkStatus();
        registryContext.addListener(registerListener);
    }

    @Override
    public <T> T getByClass(Class<T> clazz) {
        checkStatus();
        ComponentInfo info = registryContext.findByClass(clazz);
        if (info == null) {
            parser.parse(clazz,registryContext);
            info = registryContext.findByClass(clazz);

            if(info == null) {
                throw new RuntimeException("无法创建组件，解析失败：" + clazz.getName());
            }

        }

        // 禁止返回Factory组件
        if (info.isFactoryComponent()) {
            return null;
        }

        DependencyScope scope = getScope(info.getScope());
        Object target = scope.getByClass(clazz);

        if (target != null) {
            // 组件已存在，直接返回
            return (T)target;
        }

        target = create(info);

        if (info.isMultiple()) {
            scope.put(info.getName(),info.getClazz(),info.getAbstractClazz(),target);
        } else {
            scope.put(info.getName(),info.getClazz(),target);
        }

        if (info.getInitMethod() != null) {
            try {
                info.getInitMethod().invoke(target);
            } catch (Exception e) {
                throw new RuntimeException("无法初始化组件：" + info.getInitMethod(),e);
            }
        }

        return (T)target;
    }

    @Override
    public <T> T getFactory(Class clazz) {
        Object factory = factoryMap.get(clazz);
        if (factory == null) {
            if (AnnotationUtil.findAnnotation(clazz, Dependency.class) != null) {
                // 配置类型的Factory，不允许注入，只提供组件和配置。
                try {
                    factory = clazz.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("创建失败，不能初始化声明的组件",e);
                }
            } else {
                // 组件类型的Factory，需要实现Provider接口
                ComponentInfo info = registryContext.findByClass(clazz);
                if (info == null) {
                    parser.parse(clazz,registryContext);
                    info = registryContext.findByClass(clazz);

                    if(info == null) {
                        throw new RuntimeException("无法创建工厂组件，解析失败：" + clazz.getName());
                    }
                }
                factory = create(info);
                if (info.getInitMethod() != null) {
                    try {
                        info.getInitMethod().invoke(factory);
                    } catch (Exception e) {
                        throw new RuntimeException("无法初始化组件：" + info.getInitMethod(),e);
                    }
                }
            }
        }
        return (T)factory;
    }

    @Override
    public  <T> T getInternal(ComponentInfo info, Object object) {
        Object realComp = null;
        Map<Class, AnnotationDescription> objects = (Map<Class, AnnotationDescription>)object;
        if (!info.getName().equals(info.getClazz().getName())) {
            // 具名组件，名称和class的全限定名不一致。
            realComp = getHolder().getByName(info.getName());
            if (realComp == null) {
                realComp = getByName(info.getName());
            }
        } else if (info.isMultiple()) {
            // 组件是多实例的
            AnnotationDescription named = objects.get(Named.class);
            AnnotationDescription resource = objects.get(Resource.class);

            // 多实例判断
            if (named != null) {
                String name = named.getProperty(String.class,"value");
                // named注解，匹配具名组件
                realComp = getHolder().getByName(name);
                if (realComp == null) {
                    realComp = getByName(name);
                }
            }

            if (realComp == null && resource != null) {
                // 有Resources，分别进行具名和类型匹配
                String name = resource.getProperty(String.class,"name");
                if (!name.isBlank()) {
                    realComp = getHolder().getByName(name);
                    if (realComp == null) {
                        realComp = getByName(name);
                    }
                }
                Class clazz = resource.getProperty(Class.class,"type");
                if (info.getAbstractClazz().isAssignableFrom(clazz)) {
                    realComp = getHolder().getByClass(clazz);
                    if (realComp == null) {
                        realComp = this.getByClass(clazz);
                    }
                }
            }
        } else {
            // 根据类型处理
            realComp = getHolder().getByClass(info.getClazz());
            if (realComp == null) {
                realComp = getByClass(info.getClazz());
            }
        }
        return (T)realComp;
    }

    @Override
    public <T> T getByName(String name) {
        checkStatus();
        ComponentInfo info = registryContext.findByNamed(name);
        if (info == null) {
            return null;
        }
        // 禁止返回Factory组件
        if (info.isFactoryComponent()) {
            return null;
        }
        DependencyScope scope = getScope(info.getScope());
        if (scope == null) {
            return null;
        }
        Object target = scope.getByName(name);
        if (target == null) {

            target = create(info);

            if (info.isMultiple()) {
                scope.put(info.getName(),info.getClazz(),info.getAbstractClazz(),target);
            } else {
                scope.put(info.getName(),info.getClazz(),target);
            }

            if (info.getInitMethod() != null) {
                try {
                    info.getInitMethod().invoke(target);
                } catch (Exception e) {
                    throw new RuntimeException("无法初始化组件：" + info.getInitMethod(),e);
                }
            }

        }

        return (T)target;
    }

    @Override
    public <T> List<T> getByAbstract(Class<T> parent) {
        checkStatus();
        List<ComponentInfo> infoList = registryContext.findByAbstract(parent);
        if (infoList == null || infoList.size() == 0) {
            return Collections.emptyList();
        }

        DependencyScope scope = getScope(infoList.get(0).getScope());
        List result = scope.getByAbstract(parent);
        if (result != null && result.size() > 0) {
            return result;
        }

        result = new ArrayList();

        for (ComponentInfo info: infoList) {
            // 禁止返回Factory组件
            if (info.isFactoryComponent()) {
                continue;
            }
            Object target = create(info);

            // 进行Setter和字段的注入
            scope.put(info.getName(),info.getClazz(),info.getAbstractClazz(),target);

            result.add(target);
            if (info.getInitMethod() != null) {
                try {
                    info.getInitMethod().invoke(target);
                } catch (Exception e) {
                    throw new RuntimeException("无法初始化组件：" + info.getInitMethod(),e);
                }
            }
        }

        return result;
    }

    @Override
    public List<Object> getAllComponent() {
        return scopes.values().stream()
                .map(DependencyScope::getAllComponent)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }


    @Override
    public DependencyScope getScope(Class scopeType) {
        checkStatus();
        if (scopes.containsKey(scopeType)) {
            return scopes.get(scopeType);
        }
        // 没有Scope，解析Scope注解尝试创建
        ScopeImplement implInfo = (ScopeImplement) scopeType.getAnnotation(ScopeImplement.class);
        if (implInfo == null) {
            throw new RuntimeException("找不到合适的Scope，请在自定义的Scope注解添加" +
                    "ScopeImplement注解并且实现它。\n" +
                    "can not found suitable scope, please using ScopeImplement " +
                    "annotation and implement the scope。");
        }
        try {
            DependencyScope scope = (DependencyScope) implInfo.value().getConstructor().newInstance();
            scopes.put(scopeType,scope);
            return scope;
        } catch (Exception e) {
            throw new RuntimeException("无法创建Scope，异常如下：",e);
        }
    }

    @Override
    public void addListener(AfterCreationListener listener) {
        if (!afterCreationListeners.contains(listener)) {
            afterCreationListeners.add(listener);
        }
    }

    @Override
    public void removeListener(AfterCreationListener listener) {
        afterCreationListeners.remove(listener);
    }

    @Override
    public List<AfterCreationListener> getAllListeners() {
        return afterCreationListeners.stream()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void close() throws Exception {
       List<Object> components = this.getAllComponent();
       for (Object object: components) {
           ComponentInfo info = registryContext.findByClass(object.getClass());
           if (info.getDestroyMethod() != null) {
               info.getDestroyMethod().invoke(object);
           }
       }
       for (Object factory:factoryMap.values()) {
           ComponentInfo info = registryContext.findByClass(factory.getClass());
           if (info.getDestroyMethod() != null) {
               info.getDestroyMethod().invoke(factory);
           }
       }
        closed.set(true);
    }

    private void checkStatus() {
        if (closed.getAcquire()) {
            throw new RuntimeException("环境已经关闭！");
        }
    }

}
