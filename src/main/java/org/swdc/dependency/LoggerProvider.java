package org.swdc.dependency;

import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 本方法为依赖环境内的所有组件提供基于Slf4j的Logger注入，
 * 因此请在实现一种新的Application类型时将本Provider添加到
 * 依赖环境中。
 */
public class LoggerProvider implements Provider<Logger> {

    /**
     * 已经创建的Logger的缓存。
     */
    private ConcurrentHashMap<Class, Logger> loggerOfClasses = new ConcurrentHashMap<>();

    /**
     * 通过JDK代理技术，我会缓存每一个Logger，
     * 代理的Logger将会从缓存的Logger中按照Class提取真正的Logger
     * 进行使用，如果它不存在，那么将会被创建。
     *
     * @return
     */
    @Override
    public Logger get() {

        InvocationHandler handler = ((proxy, method, args) -> {
            // 正在使用Logger的Class
            Class targetClazz = method.getDeclaringClass();
            if (loggerOfClasses.contains(targetClazz)) {
                // 使用已经缓存好的Logger，调用对应的方法。
                return method.invoke(loggerOfClasses.get(targetClazz),args);
            }
            // Logger不存在，创建并缓存一个新的Logger。
            Logger logger = LoggerFactory.getLogger(targetClazz);
            loggerOfClasses.put(targetClazz,logger);
            return method.invoke(logger,args);
        });
        // 代理Logger接口，提供一个单例对象。
        return (Logger) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                new Class[] {Logger.class},
                handler);
    }

}
