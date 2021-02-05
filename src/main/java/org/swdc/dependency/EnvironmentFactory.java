package org.swdc.dependency;

import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.registry.ComponentInfo;
import org.swdc.dependency.registry.ConstructorInfo;
import org.swdc.dependency.registry.DependencyInfo;
import org.swdc.dependency.registry.FactoryDependencyInfo;
import org.swdc.dependency.scopes.CacheDependencyHolder;
import org.swdc.dependency.utils.AnnotationDescription;
import org.swdc.dependency.utils.AnnotationUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;

public abstract class EnvironmentFactory implements DependencyFactory {

    /**
     * 未创建完成的组件缓存
     */
    private CacheDependencyHolder holder = new CacheDependencyHolder();

    /**
     * 获取未创建完毕的组件缓存
     * @return 组件缓存
     */
    public CacheDependencyHolder getHolder() {
        return holder;
    }

    @Override
    public <T> T create(ComponentInfo info) {
        Class clazz = info.getClazz();
        if (holder.isCreating(clazz)) {
            throw new RuntimeException("出现了循环依赖：" + clazz.getName());
        } else {
            holder.begin(clazz);
        }
        T target = null;
        if (info.getFactory() != null) {
            target = createByFactory(info);
        } else {
            target = createByConstructor(info);
        }
        this.initialize(info,target);
        holder.complete(info);
        return target;
    }

    @Override
    public <T> T initialize(ComponentInfo info, T target) {
        List<DependencyInfo> setterDependency = info.getDependencyInfos();
        for (DependencyInfo depInfo: setterDependency) {
            if (depInfo.getField() != null) {
                // 字段注入的类型
                ComponentInfo fieldDepInfo = depInfo.getDependency()[0];

                Map<Class, AnnotationDescription> map = AnnotationUtil.getAnnotations(depInfo.getField());

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

        if (Listenable.class.isAssignableFrom(this.getClass())) {
            Listenable<AfterCreationListener> listenable = (Listenable) this;
            for (AfterCreationListener listener: listenable.getAllListeners()) {
                target = (T)listener.afterCreated(target);
            }
        }

        return target;
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

    private  <T> T createByFactory(ComponentInfo info) {
        FactoryDependencyInfo dep = info.getFactoryInfo();
        Method method = info.getFactoryMethod();

        ComponentInfo[] dependencies = dep.getDependencies();
        Object[] params = new Object[dependencies.length];
        Parameter[] parameters = method.getParameters();
        for (int idx = 0; idx < params.length; idx ++) {
            ComponentInfo param = dependencies[idx];
            Map<Class,AnnotationDescription> description = AnnotationUtil.getAnnotations(parameters[idx]);
            Object realComp = getInternal(param,description);
            if (param.isFactoryComponent()) {
                realComp = getFactory(param.getClazz());
            }
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
                Object factory = getFactory(info.getFactory());
                Object result = method.invoke(factory,params);
                holder.put(info,result);
                return (T)result;
            }
        } catch (Exception e) {
            throw new RuntimeException("创建失败：",e);
        }
    }



}
