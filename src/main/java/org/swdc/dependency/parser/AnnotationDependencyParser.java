package org.swdc.dependency.parser;

import jakarta.annotation.ManagedBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.inject.Named;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import org.swdc.dependency.registry.DependencyRegisterContext;
import org.swdc.dependency.Listenable;
import org.swdc.dependency.annotations.*;
import org.swdc.dependency.interceptor.AspectAt;
import org.swdc.dependency.listeners.AfterRegisterListener;
import org.swdc.dependency.registry.*;
import org.swdc.dependency.utils.AnnotationUtil;
import org.swdc.dependency.utils.ReflectionUtil;
import org.swdc.ours.common.annotations.AnnotationDescription;
import org.swdc.ours.common.annotations.AnnotationDescriptions;
import org.swdc.ours.common.annotations.Annotations;
import org.swdc.ours.common.type.ClassTypeAndMethods;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationDependencyParser implements DependencyParser<Class> {

    @Override
    public void parse(Class source, DependencyRegisterContext context) {

        AnnotationDescriptions annotations = Annotations.getAnnotations(source);

        if (Annotations.findAnnotationIn(annotations,Dependency.class) != null) {
            // 检查和处理声明类型的组件定义
            List<Method> methods = Stream.of(source.getMethods())
                    .filter(m -> Annotations.findAnnotation(m,Factory.class) != null)
                    .sorted(Comparator.comparingInt(Method::getParameterCount))
                    .collect(Collectors.toList());
            for (Method method: methods) {
                if (method.getAnnotation(Factory.class) != null) {
                    // 解析组件声明
                    this.parseInternalFactory(source,method,context);
                }
            }
        } else if (Annotations.findAnnotationIn(annotations,ImplementBy.class) != null){
            AnnotationDescription impl = Annotations.findAnnotationIn(annotations,ImplementBy.class);
            Class[] classes = impl.getProperty(Class[].class,"value");
            for (Class clazz: classes) {
                this.parseInternal(clazz,context);
            }
        } else {
            // 解析普通的组件信息
            parseInternal(source,context);
        }

    }

    /**
     * 解析用来声明组件的方法
     * @param method 方法
     * @param context 注册上下文
     * @return 解析完成的组件信息
     */
    private ComponentInfo parseInternalFactory(Class factoryClass,Method method,DependencyRegisterContext context) {
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
                AnnotationDescriptions annotations = Annotations.getAnnotations(param);
                String name = parseName(annotations);
                if (name != null) {
                    info = context.findByNamed(name);
                }
                if (info == null && annotations.containsKey(Resource.class)) {
                    AnnotationDescription resource = annotations.get(Resource.class);
                    Class clazz = resource.getProperty(Class.class,"type");
                    info = context.findByClass(clazz);
                    if (info == null) {
                        info = parseInternal(param.getType(),context);
                        if (info == null) {
                            throw new RuntimeException("无法解析组件，因为缺少依赖：" + param);
                        }
                    }
                }
                if (info == null) {
                    info = parseInternal(param.getType(),context);
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

            context.register(parsed);

        } else {
            // 一般来说是Provider会在这里。
            // 无Factor注解，进行默认解析

            Class source = factoryClass;
            Class provided = method.getReturnType();

            // 解析工厂提供的组件
            AnnotationDescriptions anno = Annotations.getAnnotations(provided);
            String providedName = parseNameOrDefault(anno,provided);
            Class providedScope = parseScope(anno);
            // 创建提供的组件信息
            parsed = new ComponentInfo(provided,providedName,providedScope);
            parsed.setFactory(source);
            parsed.setFactoryMethod(method);
            parsed.setFactoryInfo(new FactoryDependencyInfo(new ComponentInfo[0],false));

            Method[] methods = provided.getMethods();
            for (Method  m : methods) {
                AnnotationDescription init = Annotations.findAnnotation(m,PostConstruct.class);
                if (init != null) {
                    parsed.setInitMethod(m);
                }
                AnnotationDescription destroy = Annotations.findAnnotation(m,PreDestroy.class);
                if (destroy != null) {
                    parsed.setDestroyMethod(m);
                }
            }

            // 注册组件信息
            context.register(parsed);
        }

        // 调用Listener
        if (context instanceof Listenable) {
            Listenable<AfterRegisterListener> listenable = (Listenable<AfterRegisterListener>)context;
            parsed = this.invokeListeners(parsed,listenable.getAllListeners());
        }

        return parsed;
    }

    private ComponentInfo parseInternal(Class source, DependencyRegisterContext context) {
        AnnotationDescriptions annotations = Annotations.getAnnotations(source);
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

        // 解析构造方法
        Constructor[] constructors = source.getConstructors();
        for (Constructor constructor: constructors) {
            if (!AnnotationUtil.hasDependency(constructor)) {
                continue;
            }
            Parameter[] params = constructor.getParameters();
            ComponentInfo[] infos = new ComponentInfo[params.length];
            for (int idx = 0; idx < params.length; idx ++) {
                ComponentInfo paramParsed = parseInternal(params[idx].getType(),context);
                infos[idx] = paramParsed;
            }
            ConstructorInfo constructorInfo = new ConstructorInfo(constructor,infos);
            parsed.setConstructorInfo(constructorInfo);
            break;
        }


        // 解析方法注入
        List<Method> methods = ClassTypeAndMethods.findAllMethods(source);
        for (Method method: methods) {
            if (!AnnotationUtil.hasDependency(method)) {
                AnnotationDescriptions descriptionMap = Annotations.getAnnotations(method);
                if (descriptionMap.containsKey(PostConstruct.class)) {
                    // 解析初始化
                    parsed.setInitMethod(method);
                } else if (descriptionMap.containsKey(PreDestroy.class)) {
                    // 解析销毁方法
                    parsed.setDestroyMethod(method);
                } else if (descriptionMap.containsKey(Aspect.class)) {
                    // 解析AOP注解
                    InterceptorInfo interceptorInfo = new InterceptorInfo();
                    AnnotationDescription aspect = Annotations.findAnnotationIn(descriptionMap, Aspect.class);
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
                    AnnotationDescription order = Annotations.findAnnotationIn(descriptionMap,Order.class);
                    AspectAt aspectAt = aspect.getProperty(AspectAt.class,"at");
                    interceptorInfo.setAt(aspectAt);
                    interceptorInfo.setMethod(method);
                    if (order != null) {
                        interceptorInfo.setOrder(order.getProperty(int.class,"value"));
                    }
                    parsed.addInterceptorInfo(interceptorInfo);
                }
                AnnotationDescription aspectAnno = Annotations.findAnnotationIn(descriptionMap,With.class);
                if (aspectAnno != null) {
                    // 解析AOP的注解 - 特定的注解被标注了With，它们为本方法提供了切面。
                    Class[] interceptors = aspectAnno.getProperty(Class[].class, "aspectBy");
                    for (Class clazz : interceptors) {
                        ComponentInfo aspectInfo = context.findByClass(clazz);
                        if (aspectInfo == null) {
                            aspectInfo = this.parseInternal(clazz,context);
                        }
                        if (aspectInfo != null) {
                            parsed.addAdviceBy(aspectInfo);
                        }
                    }
                }
                continue;
            }
            Parameter[] params = method.getParameters();
            ComponentInfo[] infos = new ComponentInfo[params.length];
            for (int idx = 0; idx < params.length; idx ++) {
                ComponentInfo paramParsed = parseInternal(params[idx].getType(),context);
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
            parse(field.getType(),context);
            ComponentInfo parsedInfo = null;

            AnnotationDescriptions desc = Annotations.getAnnotations(field);
            String depName = parseName(desc);
            if (depName != null && !depName.isBlank() && !depName.isEmpty()) {
                parsedInfo = context.findByNamed(depName);
            } else {
                parsedInfo = context.findByClass(field.getType());
            }

            if (parsedInfo == null) {
                continue;
            }
            DependencyInfo dependencyInfo = new DependencyInfo(field,parsedInfo);
            parsed.getDependencyInfos().add(dependencyInfo);
        }

        if (parsed.isFactoryComponent()) {
            try {
                Method getMethod = source.getMethod("get");
                // 解析Provider组件提供的组件
                parseInternalFactory(source,getMethod,context);
            } catch (Exception e) {
                throw new RuntimeException("can not find factor method.");
            }
        }

        // 解析AOP
        AnnotationDescription aspect = Annotations.findAnnotationIn(annotations,With.class);
        if (parsed.isMultiple() && aspect == null) {
            aspect = Annotations.findAnnotation(parsed.getAbstractClazz(),With.class);
        }
        if (aspect != null) {
            Class[] interceptors = aspect.getProperty(Class[].class, "aspectBy");
            for (Class clazz : interceptors) {
                ComponentInfo aspectInfo = context.findByClass(clazz);
                if (aspectInfo == null) {
                    aspectInfo = this.parseInternal(clazz,context);
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

    private ComponentInfo getExists(Class source,AnnotationDescriptions annotations,DependencyRegisterContext context) {
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

    private Class parseScope(AnnotationDescriptions descriptionMap) {
        AnnotationDescription scopeDesc = Annotations.findAnnotationIn(descriptionMap,Scope.class);
        Class scope = null;
        if (scopeDesc == null) {
            // 默认为singleton的Scope
            scope = Singleton.class;
        } else {
            scope = scopeDesc.getDeclareOn().getAnnotation();
        }
        return scope;
    }

    private String parseNameOrDefault(AnnotationDescriptions descriptionMap, Class source) {
        String name = parseName(descriptionMap);
        if (name == null) {
            return source.getName();
        }
        return name;
    }

    private String parseName(AnnotationDescriptions descriptionMap) {
        AnnotationDescription named = Annotations.findAnnotationIn(descriptionMap,Named.class);
        if (named != null) {
            return named.getProperty(String.class,"value");
        }
        named = Annotations.findAnnotationIn(descriptionMap,Resource.class);
        if (named != null) {
            return named.getProperty(String.class,"name");
        }
        named = Annotations.findAnnotationIn(descriptionMap, ManagedBean.class);
        if (named != null) {
            return named.getProperty(String.class,"value");
        }
        return null;
    }

}
