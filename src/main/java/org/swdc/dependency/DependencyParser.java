package org.swdc.dependency;

import org.swdc.dependency.listeners.AfterRegisterListener;
import org.swdc.dependency.registry.ComponentInfo;

import java.util.Collection;
import java.util.List;

public interface DependencyParser<S> {

    List<ComponentInfo> parse(S source, DependencyRegisterContext context);

    default ComponentInfo invokeListeners(ComponentInfo info,Collection<AfterRegisterListener> listeners) {
        for (AfterRegisterListener listener: listeners) {
            info = listener.afterCreated(info);
        }
        return info;
    }

}
