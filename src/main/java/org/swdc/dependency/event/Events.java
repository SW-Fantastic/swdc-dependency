package org.swdc.dependency.event;

import org.swdc.dependency.annotations.EventListener;
import org.swdc.dependency.utils.ReflectionUtil;
import org.swdc.ours.common.annotations.AnnotationDescription;
import org.swdc.ours.common.annotations.Annotations;
import org.swdc.ours.common.type.ClassTypeAndMethods;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Events {

    private Map<Class, List<EventHandler>> eventTypeHandleMap = new HashMap<>();

    public Events() {

    }

    public void register(Class eventType, EventHandler handler) {
        List<EventHandler> handlers = eventTypeHandleMap.get(eventType);
        if (handlers == null) {
            handlers = new ArrayList<>();
        }
        handlers.add(handler);
        eventTypeHandleMap.put(eventType,handlers);
    }

    public void dispatch(AbstractEvent event) {
        Class type = event.getClass();
        List<EventHandler> handlers = eventTypeHandleMap.get(type);
        if (handlers == null) {
            return;
        }
        for (EventHandler h : handlers) {
            h.accept(event);
        }
    }

    public void registerInstance(Object instance) {
        Map<Class,List<EventHandler>> handlers = resolveHandlers(instance);
        if (handlers.size() == 0) {
            return;
        }
        for (Map.Entry<Class,List<EventHandler>> ent: handlers.entrySet()) {
            Class type = ent.getKey();
            List<EventHandler> handles = ent.getValue();
            if (handles.size() <= 0) {
                continue;
            }
            for (EventHandler h: handles) {
                this.register(type,h);
            }
        }
    }

    /**
     * 为Object生成Handler。
     * @param instance
     * @return
     */
    public static Map<Class,List<EventHandler>> resolveHandlers(Object instance) {

        Map<Class,List<EventHandler>> result = new HashMap<>();

        Class type = instance.getClass();
        List<Method> methods = ClassTypeAndMethods.findAllMethods(type);
        for (Method item: methods) {
            AnnotationDescription annoDesc = Annotations.findAnnotation(item, EventListener.class);
            if (annoDesc == null) {
                continue;
            }
            Class eventType = annoDesc.getProperty(Class.class,"type");
            if (eventType == null) {
                continue;
            }
            List<EventHandler> handlers = result.get(eventType);
            if (handlers == null) {
                handlers = new ArrayList<>();
            }

            EventHandler handler = e -> {
                try {
                    item.invoke(instance,e);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
            handlers.add(handler);
            result.put(eventType,handlers);
        }
        return result;
    }

}
