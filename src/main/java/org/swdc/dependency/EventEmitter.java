package org.swdc.dependency;

import org.swdc.dependency.event.AbstractEvent;
import org.swdc.dependency.event.Events;

public interface EventEmitter {

    <T extends AbstractEvent> void emit(T event);

    void setEvents(Events events);

}
