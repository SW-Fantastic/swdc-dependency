package org.swdc.dependency;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.swdc.dependency.event.Events;
import org.swdc.dependency.interceptor.AspectHandler;
import org.swdc.dependency.interceptor.RuntimeAspectInfo;
import org.swdc.dependency.listeners.AfterCreationListener;
import org.swdc.dependency.registry.*;
import org.swdc.dependency.scopes.CacheDependencyHolder;
import org.swdc.dependency.utils.ReflectionUtil;
import org.swdc.ours.common.type.ClassTypeAndMethods;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 组件工厂。
 *
 * 这个类专注于如何创建一个组件。
 */
public abstract class BaseEnvironmentFactory implements DependencyFactory {


    /**
     * AOP的处理所需要的ByteBuddy
     */
    private ByteBuddy byteBuddy = null;

    private boolean isNative = false;

    /**
     * 未创建完成的组件缓存
     */
    private CacheDependencyHolder holder = new CacheDependencyHolder();

    /**
     * EventBus，用于传递应用程序的事件。
     */
    private Events events = new Events();

    /**
     * 获取未创建完毕的组件缓存
     * @return 组件缓存
     */
    public CacheDependencyHolder getHolder() {
        return holder;
    }

    @Override
    public <T> T create(ComponentInfo info) {
        if (byteBuddy == null && !isNative) {
            synchronized (BaseEnvironmentFactory.class) {
                if (byteBuddy == null) {
                    try {
                        byteBuddy = new ByteBuddy();
                    } catch (Throwable e) {
                        isNative = true;
                    }
                }
            }
        }
        T target = null;
        if (info.getFactory() != null) {
            target = createByFactory(info);
        } else {
            target = createByConstructor(info);
        }

        events.registerInstance(target);
        if (EventEmitter.class.isAssignableFrom(target.getClass())) {
            EventEmitter accept = (EventEmitter) target;
            accept.setEvents(events);
        }

        if (Dynamic.class.isAssignableFrom(target.getClass())) {
            Dynamic dynamic = (Dynamic) target;
            dynamic.setContext(this);
        }

        this.initialize(info,target);

        if (info.getAdviceBy() != null && !info.getAdviceBy().isEmpty()) {
           target = this.withInterceptor(info, target, info.getAdviceBy());
        }

        return target;
    }

    /**
     * 执行AOP的增强处理。
     * @param target 目标对象
     * @param interceptors 切面的组件数据
     * @param <T> 类型
     * @return 经过增强的代理对象
     */
    private <T> T withInterceptor(ComponentInfo info,Object target, List<ComponentInfo> interceptors) {
        List<Method> methods = ClassTypeAndMethods.findAllMethods(info.getClazz());
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
            try {
                // 首先尝试加载静态代理，这个是为Graal提供的。
                // 可以通过插件进行这种代理的静态编译，得到静态的代理类。
                Class staticProxy = Class.forName(target.getClass().getName() + "$Proxied");
                Object proxied = staticProxy.getConstructor().newInstance();
                Field[] fields = staticProxy.getDeclaredFields();
                for (Field field: fields) {
                    if (InvocationHandler.class.isAssignableFrom(field.getType())) {
                        try {
                            field.setAccessible(true);
                            field.set(proxied,handler);
                        } catch (IllegalAccessException e){
                            //never happen
                            throw new RuntimeException(e);
                        }
                    }
                }
               return (T)proxied;
            } catch (ClassNotFoundException e) {
                return (T)byteBuddy.subclass(target.getClass())
                        .method(ElementMatchers.any())
                        .intercept(InvocationHandlerAdapter.of(handler))
                        .name(target.getClass().getName() + "$Proxied")
                        .make()
                        .load(target.getClass().getModule().getClassLoader())
                        .getLoaded()
                        .getConstructor()
                        .newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException("无法为组件：" + target.getClass() + "提供增强，代理对象创建失败！",e);
        }
    }

    @Override
    public <T> T initialize(ComponentInfo info, T target) {
        List<DependencyInfo> setterDependency = info.getDependencyInfos();
        for (DependencyInfo depInfo: setterDependency) {
            if (depInfo.getField() != null) {
                // 字段注入的类型
                ComponentInfo fieldDepInfo = depInfo.getDependency()[0];

                Field field = depInfo.getField();
                field.setAccessible(true);

                Object realParam = null;

                // 泛型，如果field为List或者map，
                // 这里记录List或者Map的泛型的真实类型
                if (field.getGenericType() instanceof ParameterizedType) {

                    Class paramClassType = null;
                    ParameterizedType paramType = (ParameterizedType) field.getGenericType();

                    if (List.class.isAssignableFrom(field.getType())) {
                        paramClassType = (Class) paramType.getActualTypeArguments()[0];
                    } else if (Map.class.isAssignableFrom(field.getType())) {
                        paramClassType = (Class) paramType.getActualTypeArguments()[1];
                    }

                    List<Object> params = new ArrayList<>();
                    List<ComponentInfo> infoList = findAbstractInfo(paramClassType);
                    for (ComponentInfo item : infoList) {
                        params.add(this.getInternal(item));
                    }

                    if (Map.class.isAssignableFrom(field.getType())) {
                        Class keyType = (Class) paramType.getActualTypeArguments()[0];
                        if (keyType == String.class) {
                            realParam = params.stream().collect(Collectors.toMap(c -> c.getClass().getSimpleName(), c -> c));
                        } else if (keyType == Class.class){
                            realParam = params.stream().collect(Collectors.toMap(c -> c.getClass(), c -> c));
                        } else {
                            throw new RuntimeException("map的批量注入的Key的类型只支持String和Class");
                        }
                    } else {
                        realParam = params;
                    }

                } else {
                    realParam = getInternal(fieldDepInfo);
                }

                final Object targetObject = target;

                try {
                    field.set(targetObject,realParam);
                    field.setAccessible(false);
                    continue;
                } catch (IllegalAccessException e) {
                    continue;
                }

            } else {
                ComponentInfo[] dependency = depInfo.getDependency();
                Object[] params = new Object[dependency.length];
                Method method = depInfo.getSetter();
                Parameter[] parameters = method.getParameters();
                for (int idx = 0; idx < parameters.length; idx ++) {
                    ComponentInfo methodParam = dependency[idx];
                    Object realParam = getInternal(methodParam);
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
                Object realComp = getInternal(param);
                if (realComp == null) {
                    // cache和scopes里面都没有
                    throw new RuntimeException("Missing component :" + param.getClazz().getName());
                }
                params[idx] = realComp;
            }

            try {
                Object result = constructor.newInstance(params);
                holder.put(info,result);
                return (T)result;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create component：",e);
            }

        } else {

            try {
                Constructor constructor = info.getClazz().getConstructor();
                if (constructor == null) {
                    throw new RuntimeException("No suitable constructor.");
                }
                Object result = constructor.newInstance();
                holder.put(info,result);
                return (T)result;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance ：",e);
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
        for (int idx = 0; idx < params.length; idx ++) {
            ComponentInfo param = dependencies[idx];
            Object realComp = getInternal(param);
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
