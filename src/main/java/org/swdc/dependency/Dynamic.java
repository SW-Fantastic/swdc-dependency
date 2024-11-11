package org.swdc.dependency;

public abstract class Dynamic {

    private DependencyContext context;

    void setContext(DependencyContext context) {
        this.context = context;
    }

    public <T> T getByType(Class<T> type) {
        return context.getByClass(type);
    }

}
