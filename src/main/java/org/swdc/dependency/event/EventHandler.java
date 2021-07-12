package org.swdc.dependency.event;

@FunctionalInterface
public interface EventHandler {

    void accept(AbstractEvent event);

}
