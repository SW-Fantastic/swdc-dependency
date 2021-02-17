package org.swdc.dependency.interceptor;

import java.lang.reflect.Method;

/**
 * 对外提供的执行点,不可以执行，
 * 只是包含运行时的方法的必要信息。
 *
 * 提供Before，After，AfterReturning，Exception使用。
 */
public class InvocationPoint {

    private Object[] args;
    private Method method;
    private Object component;

    public InvocationPoint(Object[] args, Object component, Method originalMethod) {
        this.args = args;
        this.component = component;
        this.method = originalMethod;
    }

    public Method getMethod() {
        return method;
    }

    public Object getComponent() {
        return component;
    }

    public Object[] getArgs() {
        return args;
    }
}
