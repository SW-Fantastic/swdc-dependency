package org.swdc.dependency.parser;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.inject.Named;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import org.swdc.dependency.DependencyParser;
import org.swdc.dependency.DependencyRegisterContext;
import org.swdc.dependency.Listenable;
import org.swdc.dependency.annotations.*;
import org.swdc.dependency.interceptor.AspectAt;
import org.swdc.dependency.listeners.AfterRegisterListener;
import org.swdc.dependency.registry.*;
import org.swdc.dependency.utils.AnnotationDescription;
import org.swdc.dependency.utils.AnnotationUtil;
import org.swdc.dependency.utils.ReflectionUtil;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationDependencyParser implements DependencyParser<Class> {

    @Override
    public List<ComponentInfo> parse(Class source, DependencyRegisterContext context) {

        List<ComponentInfo> list = new ArrayList<>();

        if (AnnotationUtil.findAnnotation(source,Dependency.class) != null) {
            // 检查和处理声明类型的组件定义
            List<Method> methods = Stream.of(source.getMethods())
                    .filter(m -> AnnotationUtil.findAnnotation(m,Factory.class) != null)
                    .sorted(Comparator.comparingInt(Method::getParameterCount))
                    .collect(Collectors.toList());
            for (Method method: methods) {
                if (method.getAnnotation(Factory.class) != null) {
                    // 解析组件声明
                    this.parseInternalFactory(method,context,list);
                }
            }
        } else {
            // 解析普通的组件信息
            parseInternal(source,context,list);
        }

        return list;
    }

    /**
     * 解析用来声明组件的方法
     * @param method 方法
     * @param context 注册上下文
     * @param container 记录返回值的列表
     * @return 解析完成的组件信息
     */
    private ComponentInfo parseInternalFactory(Method method,DependencyRegisterContext context, List<ComponentInfo> container) {
        Class type = method.getReturnType();
        Factory factory = method.getAnnotation(Factory.class);
        ComponentInfo parsed = null;
        if (factory != null) {
            // 存在Factor的注解描述，是一般的工厂方法。
            ComponentInfo[] dependInfos = new ComponentInfo[method.getParameterCount()];
            Parameter[] params = method.getParameters();
            for (int idx = 0; idx < dependInfos.length; idx ++) {
                ComponentInfo info = null;
                Parameter param = params[idx];
                Map<Class,AnnotationDescription>  annotations = AnnotationUtil.getAnnotations(param);
                String name = parseName(annotations);
                if (name != null) {
                    info = context.findByNamed(name);
                }
                if (info == null && annotations.containsKey(Resource.class)) {
                    AnnotationDescription resource = annotations.get(Resource.class);
                    Class clazz = resource.getProperty(Class.class,"type");
                    info = context.findByClass(clazz);
                    if (info == null) {
                        info = parseInternal(param.getType(),context,container);
                        if (info == null) {
                            throw new RuntimeException("无法解析组件，因为缺少依赖：" + param);
                        }
                    }
                }
                if (info == null) {
                    info = parseInternal(param.getType(),context,container);
                    if (info == null) {
                        throw new RuntimeException("无法解析组件，因为缺少依赖：" + param);
                    }
                }
                dependInfos[idx] = info;
            }
            String named = method.getName();

            Method initMethod = null;
            Method destroyMethod = null;

            try {
                if (!factory.initMethod().isBlank()) {
                    initMethod = type.getMethod(factory.initMethod());
                }
                if (!factory.destroyMethod().isBlank()) {
                    destroyMethod = type.getMethod(factory.destroyMethod());
                }
            } catch (Exception e) {
                throw new RuntimeException("无法获取指定的方法",e);
            }

            if (factory.multiple() == Object.class) {
                // 非多实现
                parsed = new ComponentInfo(type,named,factory.scope());
            } else {
                parsed = new ComponentInfo(factory.multiple(),type,named,factory.scope());
            }
            parsed.setInitMethod(initMethod);
            parsed.setDestroyMethod(destroyMethod);
            parsed.setFactory(method.getDeclaringClass());
            parsed.setFactoryMethod(method);
            parsed.setFactoryInfo(new FactoryDependencyInfo(dependInfos,Modifier.isStatic(method.getModifiers())));
            container.add(parsed);

            context.register(parsed);

        } else {
            // 一般来说是Provider会在这里。
            // 无Factor注解，进行默认解析

            Class source = method.getDeclaringClass();
            Class provided = method.getReturnType();

            // 解析工厂提供的组件
            Map<Class,AnnotationDescription> anno = AnnotationUtil.getAnnotations(provided);
            String providedName = parseNameOrDefault(anno,provided);
            Class providedScope = parseScope(anno);
            // 创建提供的组件信息
            parsed = new ComponentInfo(provided,providedName,providedScope);
            parsed.setFactory(source);
            parsed.setFactoryMethod(method);
            parsed.setFactoryInfo(new FactoryDependencyInfo(new ComponentInfo[0],false));
            // 注册组件信息
            context.register(parsed);
            container.add(parsed);
        }

        // 调用Listener
        if (context instanceof Listenable) {
            Listenable<AfterRegisterListener> listenable = (Listenable<AfterRegisterListener>)context;
            parsed = this.invokeListeners(parsed,listenable.getAllListeners());
        }

        return parsed;
    }

    private ComponentInfo parseInternal(Class source, DependencyRegisterContext context, List<ComponentInfo> container) {
        Map<Class,AnnotationDescription> annotations = AnnotationUtil.getAnnotations(source);
        // 查找已经存在的组件
        ComponentInfo info = this.getExists(source,annotations,context);
        if (info != null) {
            // 组件已存在，直接返回
            return info;
        }

        // 解析Scope
        Class scope = this.parseScope(annotations);

        // 接口或抽象类多实现的支持
        AnnotationDescription multiple = annotations.get(MultipleImplement.class);

        // 创建组件信息
        ComponentInfo parsed = null;
        String name = parseNameOrDefault(annotations,source);
        if (annotations.containsKey(MultipleImplement.class)) {
            Class type = multiple.getProperty(Class.class,"value");
            parsed = new ComponentInfo(type,source, name , scope);
        } else {
            parsed = new ComponentInfo(source,name, scope);
        }

        // 注册组件信息
        context.register(parsed);
        container.add(parsed);

        // 解析构造方法
        Constructor[] constructors = source.getConstructors();
        for (Constructor constructor: constructors) {
            if (!AnnotationUtil.hasDependency(constructor)) {
                continue;
            }
            Parameter[] params = constructor.getParameters();
            ComponentInfo[] infos = new ComponentInfo[params.length];
            for (int idx = 0; idx < params.length; idx ++) {
                ComponentInfo paramParsed = parseInternal(params[idx].getType(),context,container);
                infos[idx] = paramParsed;
            }
            ConstructorInfo constructorInfo = new ConstructorInfo(constructor,infos);
            parsed.setConstructorInfo(constructorInfo);
            break;
        }


        // 解析方法注入
        List<Method> methods = ReflectionUtil.findAllMethods(source);
        for (Method method: methods) {
            if (!AnnotationUtil.hasDependency(method)) {
                Map<Class, AnnotationDescription> descriptionMap = AnnotationUtil.getAnnotations(method);
                if (descriptionMap.containsKey(PostConstruct.class)) {
                    // 解析初始化
                    parsed.setInitMethod(method);
                } else if (descriptionMap.containsKey(PreDestroy.class)) {
                    // 解析销毁方法
                    parsed.setDestroyMethod(method);
                } else if (descriptionMap.containsKey(Aspect.class)) {
                    // 解析AOP注解
                    InterceptorInfo interceptorInfo = new InterceptorInfo();
                    AnnotationDescription aspect = AnnotationUtil.findAnnotationIn(descriptionMap, Aspect.class);
                    if (!parsed.isInterceptor()) {
                        parsed.setInterceptor(true);
                    }
                    String nameRegex = aspect.getProperty(String.class,"byNameRegex");
                    if (nameRegex != null && !nameRegex.isBlank()) {
                        interceptorInfo.setNamePattern(nameRegex);
                    }
                    Class annotationType = aspect.getProperty(Class.class,"byAnnotation");
                    if (annotationType != null && annotationType != Object.class) {
                        interceptorInfo.setAnnotationType(annotationType);
                    }
                    Class[] returnTypes = aspect.getProperty(Class[].class,"byReturnType");
                    if (returnTypes != null && returnTypes[0] != Object.class) {
                        interceptorInfo.setReturnType(returnTypes);
                    }
                    Class annotation = aspect.getProperty(Class.class,"byAnnotation");
                    if (annotation != Object.class) {
                        interceptorInfo.setAnnotationType(annotation);
                    }
                    AnnotationDescription order = AnnotationUtil.findAnnotationIn(descriptionMap,Order.class);
                    AspectAt aspectAt = aspect.getProperty(AspectAt.class,"at");
                    interceptorInfo.setAt(aspectAt);
                    interceptorInfo.setMethod(method);
                    if (order != null) {
                        interceptorInfo.setOrder(order.getProperty(int.class,"value"));
                    }
                    parsed.addInterceptorInfo(interceptorInfo);
                }
                continue;
            }
            Parameter[] params = method.getParameters();
            ComponentInfo[] infos = new ComponentInfo[params.length];
            for (int idx = 0; idx < params.length; idx ++) {
                ComponentInfo paramParsed = parseInternal(params[idx].getType(),context,container);
                infos[idx] = paramParsed;
                DependencyInfo dependencyInfo = new DependencyInfo(method,infos);
                parsed.getDependencyInfos().add(dependencyInfo);
            }
        }

        // 解析字段注入的信息
        List<Field> fields = ReflectionUtil.findDependencyFields(source);
        for (Field field: fields) {
            if(!AnnotationUtil.hasDependency(field)) {
                continue;
            }
            ComponentInfo fieldParsed = parseInternal(field.getType(),context,container);
            DependencyInfo dependencyInfo = new DependencyInfo(field,fieldParsed);
            parsed.getDependencyInfos().add(dependencyInfo);
        }

        if (parsed.isFactoryComponent()) {
            try {
                Method getMethod = source.getMethod("get");
                // 解析Provider组件提供的组件
                parseInternalFactory(getMethod,context,container);
            } catch (Exception e) {
                throw new RuntimeException("can not find factor method.");
            }
        }

        // 解析AOP
        AnnotationDescription aspect = AnnotationUtil.findAnnotationIn(annotations,With.class);
        if (parsed.isMultiple() && aspect == null) {
            aspect = AnnotationUtil.findAnnotation(parsed.getAbstractClazz(),With.class);
        }
        if (aspect != null) {
            Class[] interceptors = aspect.getProperty(Class[].class, "aspectBy");
            for (Class clazz : interceptors) {
                ComponentInfo aspectInfo = context.findByClass(clazz);
                if (aspectInfo == null) {
                    aspectInfo = this.parseInternal(clazz,context,container);
                }
                if (aspectInfo == null) {
                    continue;
                }
                parsed.addAdviceBy(aspectInfo);
            }
        }

        // 完成组件解析
        parsed.setResolved(true);
        // 调用Listener
        if (context instanceof Listenable) {
            Listenable<AfterRegisterListener> listenable = (Listenable<AfterRegisterListener>)context;
            parsed = this.invokeListeners(parsed,listenable.getAllListeners());
        }

        return parsed;

    }

    private ComponentInfo getExists(Class source,Map<Class,AnnotationDescription> annotations,DependencyRegisterContext context) {
        // 解析组件名
        AnnotationDescription named = annotations.get(Named.class);
        if (named != null) {
            // 查找已注册的数据
            ComponentInfo info = context.findByNamed(named.getProperty(String.class,"value"));
            if (info != null) {
                // 已存在，直接返回
                return info;
            }
        }
        // 按类查找
        ComponentInfo info = context.findByClass(source);
        if (info != null) {
            // 组件已存在，直接返回
            return info;
        }
        return null;
    }

    private Class parseScope(Map<Class,AnnotationDescription> descriptionMap) {
        AnnotationDescription scopeDesc = AnnotationUtil.findAnnotationIn(descriptionMap,Scope.class);
        Class scope = null;
        if (scopeDesc == null) {
            // 默认为singleton的Scope
            scope = Singleton.class;
        } else {
            scope = scopeDesc.getDeclareOn().getAnnotation();
        }
        return scope;
    }

    private String parseNameOrDefault(Map<Class,AnnotationDescription> descriptionMap, Class source) {
        String name = parseName(descriptionMap);
        if (name == null) {
            return source.getName();
        }
        return name;
    }

    private String parseName(Map<Class,AnnotationDescription> descriptionMap) {
        AnnotationDescription named = AnnotationUtil.findAnnotationIn(descriptionMap,Named.class);
        if (named != null) {
            return named.getProperty(String.class,"value");
        }
        named = AnnotationUtil.findAnnotationIn(descriptionMap,Resource.class);
        if (named != null) {
            return named.getProperty(String.class,"name");
        }
        return null;
    }

}
