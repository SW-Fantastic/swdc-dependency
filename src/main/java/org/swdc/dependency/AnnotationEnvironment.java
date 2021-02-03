package org.swdc.dependency;

import jakarta.annotation.Resource;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.swdc.dependency.annotations.ScopeImplement;
import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.listeners.AfterRegisterListener;
import org.swdc.dependency.parser.AnnotationDependencyParser;
import org.swdc.dependency.registry.*;
import org.swdc.dependency.scopes.CacheDependencyHolder;
import org.swdc.dependency.scopes.SingletonDependencyScope;
import org.swdc.dependency.utils.AnnotationDescription;
import org.swdc.dependency.utils.AnnotationUtil;

import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AnnotationEnvironment implements DependencyEnvironment,Listenable<AfterCreationListener> {

    private Map<Class, DependencyScope> scopes;
    private DefaultDependencyRegistryContext registryContext;
    private DependencyParser<Class> parser;

    private Map<Class, Object> factoryMap;

    private List<AfterCreationListener> afterCreationListeners;

    private CacheDependencyHolder holder = new CacheDependencyHolder();

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

    public AnnotationEnvironment withDependency(Class dependency) {
        this.parser.parse(dependency,registryContext);
        return this;
    }

    public AnnotationEnvironment withScope(DependencyScope scope) {
        checkStatus();
        this.registerScope(scope);
        return this;
    }

    public AnnotationEnvironment withComponent(Class clazz) {
        checkStatus();
        ComponentInfo info = this.registryContext.findByClass(clazz);
        if (info == null) {
            parser.parse(clazz,this.registryContext);
        }
        return this;
    }

    public AnnotationEnvironment afterRegister(AfterRegisterListener listener) {
        checkStatus();
        registryContext.addListener(listener);
        return this;
    }

    public AnnotationEnvironment afterCreation(AfterCreationListener listener) {
        checkStatus();
        this.addListener(listener);
        return this;
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

        DependencyScope scope = getScope(info.getScope());
        Object target = scope.getByClass(clazz);

        if (target != null) {
            // 组件已存在，直接返回
            return (T)target;
        }

        if (holder.isCreating(clazz)) {
            throw new RuntimeException("出现了循环依赖：" + clazz.getName());
        } else {
            holder.begin(clazz);
        }

        if (info.getFactoryInfo() != null) {
            target = createByFactory(info);
        } else {
            target = createByConstructor(info);
        }

        // 进行Setter和字段的注入
        target = initialize(info,target);

        if (info.isMultiple()) {
            scope.put(info.getName(),info.getClazz(),info.getAbstractClazz(),target);
        } else {
            scope.put(info.getName(),info.getClazz(),target);
        }

        holder.complete(info);

        if (info.getInitMethod() != null) {
            try {
                info.getInitMethod().invoke(target);
            } catch (Exception e) {
                throw new RuntimeException("无法初始化组件：" + info.getInitMethod(),e);
            }
        }

        return (T)target;
    }

    private <T> T initialize(ComponentInfo info, T target) {
        List<DependencyInfo> setterDependency = info.getDependencyInfos();
        for (DependencyInfo depInfo: setterDependency) {
            if (depInfo.getField() != null) {
                // 字段注入的类型
                ComponentInfo fieldDepInfo = depInfo.getDependency()[0];

                Map<Class,AnnotationDescription> map = AnnotationUtil.getAnnotations(depInfo.getField());

                final Object realParam = getInternal(fieldDepInfo,map);
                final Object targetObject = target;

                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    try {
                        Field field = depInfo.getField();
                        field.setAccessible(true);
                        field.set(targetObject,realParam);
                        field.setAccessible(false);
                        return null;
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                });

            } else {
                ComponentInfo[] dependency = depInfo.getDependency();
                Object[] params = new Object[dependency.length];
                Method method = depInfo.getSetter();
                Parameter[] parameters = method.getParameters();
                for (int idx = 0; idx < parameters.length; idx ++) {
                    Map<Class,AnnotationDescription> map = AnnotationUtil.getAnnotations(depInfo.getSetter());
                    ComponentInfo methodParam = dependency[idx];
                    Object realParam = getInternal(methodParam,map);
                    if (realParam == null) {
                        throw new RuntimeException("无法初始化实例，因为缺少组件:" + methodParam.getClazz().getName());
                    }
                    params[idx] = realParam;
                }
                try {
                    method.invoke(target,params);
                } catch (Exception e) {
                    throw new RuntimeException("初始化实例失败:", e);
                }
            }
        }

        for (AfterCreationListener listener: afterCreationListeners) {
            target = (T)listener.afterCreated(target);
        }

        return target;
    }

    private <T> T getInternal(ComponentInfo info, Map<Class, AnnotationDescription> objects) {

        Object realComp = null;

        if (!info.getName().equals(info.getClazz().getName())) {
            // 具名组件，名称和class的全限定名不一致。
            realComp = Optional.ofNullable(getByName(info.getName()))
                    .orElse(holder.getByName(info.getName()));
        } else if (info.isMultiple()) {
            // 组件是多实例的
            AnnotationDescription named = objects.get(Named.class);
            AnnotationDescription resource = objects.get(Resource.class);

            // 多实例判断
            if (named != null) {
                String name = named.getProperty(String.class,"value");
                // named注解，匹配具名组件
                realComp = Optional.ofNullable(getByName(name))
                        .orElse(holder.getByName(name));
            }

            if (realComp == null && resource != null) {
                // 有Resources，分别进行具名和类型匹配
                String name = resource.getProperty(String.class,"name");
                if (!name.isBlank()) {
                    realComp = Optional.ofNullable(getByName(name))
                            .orElse(holder.getByName(name));
                }
                Class clazz = resource.getProperty(Class.class,"type");
                if (info.getAbstractClazz().isAssignableFrom(clazz)) {
                    realComp = getByClass(clazz);
                    if (realComp == null) {
                        realComp = holder.getByClass(clazz);
                    }
                }
            }
        } else {
            // 根据类型处理
            realComp = Optional.ofNullable(getByClass(info.getClazz()))
                    .orElse(holder.getByClass(info.getClazz()));
        }
        return (T)realComp;
    }

    private <T> T createByConstructor(ComponentInfo info) {

        if (info.getConstructorInfo() != null) {

            ConstructorInfo constructorInfo = info.getConstructorInfo();

            ComponentInfo[] dependencies = constructorInfo.getDependencies();
            Object[] params = new Object[dependencies.length];
            Constructor constructor = constructorInfo.getConstructor();
            Parameter[] parameters = constructor.getParameters();

            for (int idx = 0; idx < params.length; idx ++) {
                ComponentInfo param = dependencies[idx];
                Map<Class,AnnotationDescription> description = AnnotationUtil.getAnnotations(parameters[idx]);
                Object realComp = getInternal(param,description);
                if (realComp == null) {
                    // cache和scopes里面都没有
                    throw new RuntimeException("无法创建实例，因为缺少组件:" + param.getClazz().getName());
                }
                params[idx] = realComp;
            }

            try {
                Object result = constructor.newInstance(params);
                holder.put(info,result);
                return (T)result;
            } catch (Exception e) {
                throw new RuntimeException("创建失败：",e);
            }

        } else {

            try {
                Constructor constructor = info.getClazz().getConstructor();
                if (constructor == null) {
                    throw new RuntimeException("无法创建实例，因为没有合适的构造方法。");
                }
                Object result = constructor.newInstance();
                holder.put(info,result);
                return (T)result;
            } catch (Exception e) {
                throw new RuntimeException("创建失败，原因是：",e);
            }

        }
    }

    public <T> T createByFactory(ComponentInfo info) {
        FactoryDependencyInfo dep = info.getFactoryInfo();
        Method method = info.getFactoryMethod();

        ComponentInfo[] dependencies = dep.getDependencies();
        Object[] params = new Object[dependencies.length];
        Parameter[] parameters = method.getParameters();
        for (int idx = 0; idx < params.length; idx ++) {
            ComponentInfo param = dependencies[idx];
            Map<Class,AnnotationDescription> description = AnnotationUtil.getAnnotations(parameters[idx]);
            Object realComp = getInternal(param,description);
            if (realComp == null) {
                // cache和scopes里面都没有
                throw new RuntimeException("无法创建实例，因为缺少组件:" + param.getClazz().getName());
            }
            params[idx] = realComp;
        }
        try {
            if (dep.isStatic()) {
                Object result = method.invoke(null,params);
                holder.put(info,result);
                return (T)result;
            } else {
                Object factory = factoryMap.get(info.getFactory());
                if (factory == null) {
                    factory = info.getFactory().getConstructor().newInstance();
                    factoryMap.put(info.getFactory(),factory);
                }
                Object result = method.invoke(factory,params);
                holder.put(info,result);
                return (T)result;
            }
        } catch (Exception e) {
            throw new RuntimeException("创建失败：",e);
        }
    }


    @Override
    public <T> T getByName(String name) {
        checkStatus();
        ComponentInfo info = registryContext.findByNamed(name);
        if (info == null) {
            return null;
        }
        DependencyScope scope = getScope(info.getScope());
        if (scope == null) {
            return null;
        }
        Object target = scope.getByName(name);
        if (target == null) {

            holder.begin(info.getClazz());
            if (info.getFactoryInfo() != null) {
                target = this.createByFactory(info);
            } else {
                target = this.createByConstructor(info);
            }

            this.initialize(info,target);

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

            holder.complete(info);
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
            if (!holder.isCreating(info.getClazz())) {
                holder.begin(info.getClazz());
            } else {
                throw new RuntimeException("发现了循环依赖：" + info.getClazz().getName());
            }

            Object target = null;

            if (info.getFactoryInfo() != null) {
                target = createByFactory(info);
            } else {
                target = createByConstructor(info);
            }

            // 进行Setter和字段的注入
            target = initialize(info,target);
            scope.put(info.getName(),info.getClazz(),info.getAbstractClazz(),target);

            holder.complete(info);
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
            DependencyScope scope = implInfo.value().getConstructor().newInstance();
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
        closed.set(true);
    }

    private void checkStatus() {
        if (closed.getAcquire()) {
            throw new RuntimeException("环境已经关闭！");
        }
    }

}
