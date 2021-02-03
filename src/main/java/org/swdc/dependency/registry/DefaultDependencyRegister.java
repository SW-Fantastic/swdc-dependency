package org.swdc.dependency.registry;

import org.swdc.dependency.DependencyRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultDependencyRegister implements DependencyRegister {

    /**
     * 总注册表。
     * 所有的被分析出来的类都放这里。
     */
    protected ConcurrentHashMap<Class,ComponentInfo> defaultInfo = new ConcurrentHashMap<>();

    /**
     * 多实现注册表。
     * 所有的，对于一个接口或抽象类的多实现的形式的都放这里。
     */
    protected ConcurrentHashMap<Class, List<ComponentInfo>> abstractInfo = new ConcurrentHashMap<>();

    /**
     * 命名注册表。
     * 所有具名组件（被Named注解的）都放在这里。
     */
    protected ConcurrentHashMap<String, ComponentInfo> namedInfo = new ConcurrentHashMap<>();

    /**
     * 作用域注册表。
     * 具有指定作用域的组件都放在这里。
     */
    protected ConcurrentHashMap<Class,List<ComponentInfo>> scopedInfo = new ConcurrentHashMap<>();

    @Override
    public ComponentInfo register(ComponentInfo info) {
        if (info.isRegistered()) {
            return info;
        }
        defaultInfo.put(info.getClazz(),info);
        if (!info.getName().equals(info.getClazz().getName())) {
            namedInfo.put(info.getName(),info);
        }
        if (info.isMultiple()) {
            Class implFor = info.getAbstractClazz();
            List<ComponentInfo> infoList = abstractInfo.getOrDefault(implFor,new ArrayList<>());
            infoList.add(info);
            abstractInfo.put(implFor,infoList);
        }
        List<ComponentInfo> infoList = scopedInfo.getOrDefault(info.getScope(),new ArrayList<>());
        infoList.add(info);
        scopedInfo.put(info.getScope(),infoList);
        info.setRegistered(true);
        return info;
    }

}
