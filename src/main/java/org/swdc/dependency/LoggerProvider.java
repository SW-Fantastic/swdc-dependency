package org.swdc.dependency;

import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LoggerProvider implements Provider<Logger> {

    private ConcurrentHashMap<Class, Logger> loggerOfClasses = new ConcurrentHashMap<>();

    @Override
    public Logger get() {


        InvocationHandler handler = ((proxy, method, args) -> {
            Class targetClazz = method.getDeclaringClass();
            if (loggerOfClasses.contains(targetClazz)) {
                return method.invoke(loggerOfClasses.get(targetClazz),args);
            }
            Logger logger = LoggerFactory.getLogger(targetClazz);
            loggerOfClasses.put(targetClazz,logger);
            return method.invoke(logger,args);
        });
        return (Logger) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                new Class[] {Logger.class},
                handler);
    }

}
