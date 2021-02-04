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
import org.swdc.dependency.annotations.Dependency;
import org.swdc.dependency.annotations.Factory;
import org.swdc.dependency.annotations.MultipleImplement;
import org.swdc.dependency.listeners.AfterRegisterListener;
import org.swdc.dependency.registry.ComponentInfo;
import org.swdc.dependency.registry.ConstructorInfo;
import org.swdc.dependency.registry.DependencyInfo;
import org.swdc.dependency.registry.FactoryDependencyInfo;
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

        if (source.getAnnotation(Dependency.class) != null) {
            List<Method> methods = Stream.of(source.getMethods())
                    .filter(m -> m.getAnnotation(Factory.class) != null)
                    .sorted(Comparator.comparingInt(Method::getParameterCount))
                    .collect(Collectors.toList());
            for (Method method: methods) {
                if (method.getAnnotation(Factory.class) != null) {
                    this.parseInternalFactory(method,context,list);
                }
            }
        } else {
            parseInternal(source,context,list);
        }

        return list;
    }

    private ComponentInfo parseInternalFactory(Method method,DependencyRegisterContext context, List<ComponentInfo> container) {
        Class type = method.getReturnType();
        Factory factory = method.getAnnotation(Factory.class);
        ComponentInfo[] dependInfos = new ComponentInfo[method.getParameterCount()];
        Parameter[] params = method.getParameters();
        for (int idx = 0; idx < dependInfos.length; idx ++) {
            ComponentInfo info = null;
            Parameter param = params[idx];
            Map<Class,AnnotationDescription>  annotations = AnnotationUtil.getAnnotations(param);
            if (annotations.containsKey(Named.class)) {
                AnnotationDescription named = annotations.get(Named.class);
                info = context.findByNamed(named.getProperty(String.class,"value"));
            }
            if (info == null && annotations.containsKey(Resource.class)) {
                AnnotationDescription resource = annotations.get(Resource.class);
                String named = resource.getProperty(String.class,"name");
                Class clazz = resource.getProperty(Class.class,"type");
                if (!named.isBlank()) {
                    info = context.findByNamed(named);
                } else {
                    info = context.findByClass(clazz);
                }
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
        ComponentInfo parsed = null;

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

        return parsed;
    }

    private ComponentInfo parseInternal(Class source, DependencyRegisterContext context, List<ComponentInfo> container) {
        Map<Class,AnnotationDescription> annotations = AnnotationUtil.getAnnotations(source);
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

        // 解析Scope
        AnnotationDescription scopeDesc = AnnotationUtil.findAnnotationIn(annotations,Scope.class);
        Class scope = null;
        if (scopeDesc == null) {
            // 默认为singleton的Scope
            scope = Singleton.class;
        } else {
            scope = scopeDesc.getDeclareOn().getAnnotation();
        }

        // 接口或抽象类多实现的支持
        AnnotationDescription multiple = annotations.get(MultipleImplement.class);

        // 创建组件信息
        ComponentInfo parsed = null;

        if (annotations.containsKey(MultipleImplement.class)) {
            Class type = multiple.getProperty(Class.class,"value");
            String name = null;
            if (named != null){
                name = named.getProperty(String.class,"value");
            } else {
                name = source.getName();
            }
            parsed = new ComponentInfo(type,source, name , scope);
        } else {
            String name = null;
            if (named != null){
                name = named.getProperty(String.class,"value");
            } else {
                name = source.getName();
            }
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
        List<Method> methods = ReflectionUtil.findDependencyMethods(source);
        for (Method method: methods) {
            if (!AnnotationUtil.hasDependency(method)) {
                Map<Class, AnnotationDescription> descriptionMap = AnnotationUtil.getAnnotations(method);
                if (descriptionMap.containsKey(PostConstruct.class)) {
                    parsed.setInitMethod(method);
                } else if (descriptionMap.containsKey(PreDestroy.class)) {
                    parsed.setDestroyMethod(method);
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

        // 完成组件解析
        parsed.setResolved(true);
        // 调用Listener
        if (context instanceof Listenable) {
            Listenable<AfterRegisterListener> listenable = (Listenable<AfterRegisterListener>)context;
            parsed = this.invokeListeners(parsed,listenable.getAllListeners());
        }

        return parsed;

    }

}
