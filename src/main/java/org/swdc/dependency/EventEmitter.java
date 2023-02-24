package org.swdc.dependency;

import org.swdc.dependency.event.AbstractEvent;
import org.swdc.dependency.event.Events;

/**
 * 提供EventBus的功能，通常一个Application会在特定的
 * 组件中实现本接口以提供应用程序内的Event的处理。
 */
public interface EventEmitter {

    /**
     * 发送一个基于AbstractEvent的事件，
     * 通过调用Events的emit来完成事件的发送。
     *
     * @param event 事件对象
     * @param <T> 事件的消息类型
     */
    <T extends AbstractEvent> void emit(T event);

    /**
     * 提供一个EventBus，把参数放入字段中，并且调用
     * 它的emit方法就能够发送event了。
     *
     * @param events EventBus对象
     */
    void setEvents(Events events);

}
