package org.swdc.dependency;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.swdc.dependency.interceptor.AspectHandler;
import org.swdc.dependency.interceptor.RuntimeAspectInfo;
import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.registry.*;
import org.swdc.dependency.scopes.CacheDependencyHolder;
import org.swdc.dependency.utils.AnnotationDescription;
import org.swdc.dependency.utils.AnnotationUtil;
import org.swdc.dependency.utils.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 组件工厂。
 *
 * 这个类专注于如何创建一个组件。
 */
public abstract class EnvironmentFactory implements DependencyFactory {


    /**
     * AOP的处理所需要的ByteBuddy
     */
    private ByteBuddy byteBuddy = new ByteBuddy();

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

        if (info.getAdviceBy() != null && !info.getAdviceBy().isEmpty()) {
           target = this.withInterceptor(target, info.getAdviceBy());
        }

        holder.complete(info);
        return target;
    }

    /**
     * 执行AOP的增强处理。
     * @param target 目标对象
     * @param interceptors 切面的组件数据
     * @param <T> 类型
     * @return 经过增强的代理对象
     */
    private <T> T withInterceptor(Object target, List<ComponentInfo> interceptors) {
        List<Method> methods = ReflectionUtil.findAllMethods(target.getClass());
        Map<Method,List<RuntimeAspectInfo>> processPoints = new HashMap<>();

        // 整理AOP拦截数据
        List<InterceptorInfo> infos = interceptors.stream()
                .map(ComponentInfo::getInterceptorInfos)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        for (Method method: methods) {
            // 过滤，匹配拦截器中对应的拦截方法
            List<RuntimeAspectInfo> proxies = infos.stream()
                    .filter(i -> i.match(method))
                    .map(i -> {
                        // 创建方法的执行点
                        Class interceptorClazz = i.getMethod().getDeclaringClass();
                        Object interceptor = this.getInterceptor(interceptorClazz);
                        if (interceptorClazz == null) {
                            return null;
                        }
                        return new RuntimeAspectInfo(interceptor,method,i.getMethod(),i.getAt(),i.getOrder());
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            processPoints.put(method,proxies);
        }

        AspectHandler handler = new AspectHandler(target,processPoints);

        try {
            target = byteBuddy.subclass(target.getClass())
                    .method(ElementMatchers.any())
                    .intercept(InvocationHandlerAdapter.of(handler))
                    .make()
                    .load(target.getClass().getModule().getClassLoader())
                    .getLoaded()
                    .getConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException("无法为组件：" + target.getClass() + "提供增强，代理对象创建失败！",e);
        }
        return (T)target;
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

    /**
     * 通过构造方法创建组件
     * @param info 组件信息
     * @param <T> 组件类型
     * @return 创建好的未初始化的组件
     */
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

    /**
     * 通过工厂的方式创建对象。
     *
     * @param info 组件信息
     * @param <T> 类型
     * @return 创建好的未初始化的组件。
     */
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
