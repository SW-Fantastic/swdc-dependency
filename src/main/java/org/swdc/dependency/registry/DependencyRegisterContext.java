package org.swdc.dependency.registry;

import org.swdc.dependency.registry.ComponentInfo;
import org.swdc.dependency.registry.DependencyRegister;

import java.util.List;

public interface DependencyRegisterContext extends DependencyRegister {

    ComponentInfo findByClass(Class clazz);

    ComponentInfo findByNamed(String named);

    List<ComponentInfo> findByAbstract(Class clazz);

    List<ComponentInfo> findByScope(Class scope);

    List<ComponentInfo> findAll();

}
