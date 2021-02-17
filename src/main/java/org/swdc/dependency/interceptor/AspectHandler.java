package org.swdc.dependency.interceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AspectHandler implements InvocationHandler {

    private Object target;
    private Map<Method,List<RuntimeAspectInfo>> processPoints;

    public AspectHandler(Object target, Map<Method,List<RuntimeAspectInfo>> points) {
        this.target = target;
        this.processPoints = points;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        List<RuntimeAspectInfo> processPoints = this.processPoints.get(method);
        if (processPoints == null || processPoints.size() == 0) {
            return method.invoke(target,args);
        }
        Map<AspectAt,List<RuntimeAspectInfo>> advices = processPoints.stream()
                .collect(Collectors.groupingBy(RuntimeAspectInfo::getAspectAt));
        InvocationPoint invocation = new InvocationPoint(args,target,method);
        try {

            Object result = null;

            List<RuntimeAspectInfo> before = advices.get(AspectAt.BEFORE);
            if (before != null && before.size() > 0) {
                before = before.stream().sorted(Comparator.comparingInt(RuntimeAspectInfo::getOrder))
                        .collect(Collectors.toList());
                for (RuntimeAspectInfo point: before) {
                    point.getAspectInvocation().invoke(point.getInterceptor(),invocation);
                }
            }

            List<RuntimeAspectInfo> around = advices.get(AspectAt.AROUND);
            if (around != null && around.size() > 0) {
                around = around.stream().sorted(Comparator.comparingInt(RuntimeAspectInfo::getOrder))
                        .collect(Collectors.toList());
                ProcessPoint processPoint = ProcessPoint.resolve(0,target,method,args,around);
                result = processPoint.process();
            } else {
                ProcessPoint processPoint = ProcessPoint.resolve(0,target,method,args, Collections.emptyList());
                result = processPoint.process();
            }

            List<RuntimeAspectInfo> after = advices.get(AspectAt.AFTER);
            if (after != null && after.size() > 0) {
                after = after.stream().sorted(Comparator.comparingInt(RuntimeAspectInfo::getOrder))
                        .collect(Collectors.toList());
                for (RuntimeAspectInfo point: after) {
                    point.getAspectInvocation().invoke(point.getInterceptor(),invocation);
                }
            }

            return result;

        } catch (Exception e) {
            List<RuntimeAspectInfo> afterThrowing = advices.get(AspectAt.AFTER_THROWING);
            if (afterThrowing != null && afterThrowing.size() > 0) {
                afterThrowing = afterThrowing.stream().sorted(Comparator.comparingInt(RuntimeAspectInfo::getOrder))
                        .collect(Collectors.toList());
                for (RuntimeAspectInfo point: afterThrowing) {
                    point.getAspectInvocation().invoke(point.getInterceptor(),invocation);
                }
            }
        } finally {
            List<RuntimeAspectInfo> afterReturn = advices.get(AspectAt.AFTER_RETURNING);
            if (afterReturn != null && afterReturn.size() > 0) {
                afterReturn = afterReturn.stream().sorted(Comparator.comparingInt(RuntimeAspectInfo::getOrder))
                        .collect(Collectors.toList());
                for (RuntimeAspectInfo point: afterReturn) {
                    point.getAspectInvocation().invoke(point.getInterceptor(),invocation);
                }
            }
        }
        return target;
    }

}
