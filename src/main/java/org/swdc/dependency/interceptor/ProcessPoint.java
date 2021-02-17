package org.swdc.dependency.interceptor;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 可执行的执行点，提供给Around使用。
 */
public class ProcessPoint  {

    private ProcessPoint next;
    private Object[] args;
    private Method method;
    private Object component;

    public void setNext(ProcessPoint next) {
        this.next = next;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object process() throws Throwable{
       return method.invoke(getComponent(),getArgs());
    }

    public void setComponent(Object component) {
        this.component = component;
    }

    public Object getComponent() {
        return component;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }

    public static ProcessPoint resolve(int index, Object target, Method methodOriginal, Object[] args, List<RuntimeAspectInfo> infos){

        ProcessPoint processPoint = new ProcessPoint();

        if (index > infos.size()) {
            throw new RuntimeException("解析失败，出现了未知错误");
        } else if (index == infos.size()) {
            processPoint.setMethod(methodOriginal);
            processPoint.setArgs(args);
            processPoint.setComponent(target);
            return processPoint;
        }

        ProcessPoint next = resolve(index + 1,target,methodOriginal,args,infos);
        RuntimeAspectInfo info = infos.get(index);
        ProcessPoint result = new ProcessPoint();
        result.setNext(next);
        result.setMethod(info.getAspectInvocation());
        result.setComponent(info.getInterceptor());
        result.setArgs(new Object[]{ next });
        return result;
    }

}
