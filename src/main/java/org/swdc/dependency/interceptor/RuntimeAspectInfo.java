package org.swdc.dependency.interceptor;

import java.lang.reflect.Method;

/**
 * AOP的执行点。
 * 包含一个AOP的切面方法执行时候需要的全部信息。
 */
public class RuntimeAspectInfo {

    private Object interceptor;

    private Method invocation;

    private Method aspectInvocation;

    private AspectAt aspectAt;

    private Class interceptorClass;

    private int order = 0;

    public RuntimeAspectInfo(Object interceptor,
                             Method invocation, Method aspectInvocation,
                             AspectAt aspectAt, int order) {
        this.interceptorClass = invocation.getDeclaringClass();
        this.interceptor = interceptor;
        this.invocation = invocation;
        this.aspectAt = aspectAt;
        this.aspectInvocation = aspectInvocation;
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public Method getInvocation() {
        return invocation;
    }

    public Method getAspectInvocation() {
        return aspectInvocation;
    }

    public AspectAt getAspectAt() {
        return aspectAt;
    }

    public Class getInterceptorClass() {
        return interceptorClass;
    }

    public Object getInterceptor() {
        return interceptor;
    }

}
