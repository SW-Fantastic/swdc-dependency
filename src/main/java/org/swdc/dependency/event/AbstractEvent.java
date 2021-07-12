package org.swdc.dependency.event;

public abstract class AbstractEvent {

    private Object message;

    public AbstractEvent(Object message) {
        this.message = message;
    }

    public <T> T getMessage() {
        return (T)message;
    }

}
