package org.swdc.dependency.layer;

import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.DependencyFactory;
import org.swdc.dependency.registry.ComponentInfo;

import java.util.ArrayList;
import java.util.List;

public class Layer implements DependencyLayer {

    /**
     * 父组件
     */
    private Layer parent;

    /**
     * DependencyContext的可分层实现。
     */
    private Layerable layerable;


    /**
     *  环境的封闭性。
     *
     *  含义是是否能够解析和创建新组件。
     *  一般来说，被封装的Environment不应该出现
     *  被封装的组件之外的内容，环境相对独立，所以，
     *  应该将close置为true，阻止新组件在此层上的解析。
     *
     *  应用的底层不能封闭，这一层一般是应用程序本身所在的层，
     *  这一层向上引用被封装的其他层。
     */
    private boolean closed;

    /**
     * 类型参照列表，exports方法提供的ComponentInfo的Class类型
     * 会存放在这里。
     */
    private List<Class> typeReference = new ArrayList<>();

    /**
     * 抽象参照列表，exports方法提供的抽象类的class会存放在这里。
     */
    private List<Class> abstractReference = new ArrayList<>();

    /**
     * 具名参照列表，本层exports方法提供的组件名会存放在这里
     */
    private List<String> namedReference = new ArrayList<>();

    /**
     * 工厂参照列表，本层exports方法提供的工厂组件的class会放在这里
     */
    private List<Class> factoryReference = new ArrayList<>();

    /**
     * AOP增强参照列表，本层exports方法提供的AOP增强组件的class会存放在这里。
     */
    private List<Class> interceptorReference = new ArrayList<>();

    /**
     * 创建开放层
     * @param layerable 可分层的Context
     */
    public Layer(DependencyContext layerable) {
        if (!Layerable.class.isAssignableFrom(layerable.getClass())) {
            throw new RuntimeException("该DependencyContext不支持分层。");
        }
        this.layerable = (Layerable) layerable;
    }

    /**
     * 创建封闭层
     * @param layerable 可分层的Context
     * @param closed 封闭性
     */
    private Layer(DependencyContext layerable, boolean closed) {
        if (!Layerable.class.isAssignableFrom(layerable.getClass())) {
            throw new RuntimeException("DependencyContext不支持分层。");
        }
        this.layerable = (Layerable) layerable;
        this.closed = closed;
        List<ComponentInfo> infoList = this.layerable.getExport();
        for (ComponentInfo info: infoList) {
            typeReference.add(info.getClazz());
            if (!info.getName().equals(info.getClazz().getName())) {
                namedReference.add(info.getName());
            } else if (info.isMultiple()) {
                abstractReference.add(info.getClazz());
            } else if (info.isInterceptor()) {
                interceptorReference.add(info.getClazz());
            } else if (info.isFactoryComponent()) {
                factoryReference.add(info.getClazz());
            }
        }
    }

    /**
     * 使用Context添加新的层
     * @param layerable 可分层Context
     * @return 本对象
     */
    public Layer based(DependencyContext layerable) {
        if (!Layerable.class.isAssignableFrom(layerable.getClass())) {
            throw new RuntimeException("DependencyContext不支持分层。");
        }
        Layer layer = this;
        while (layer.parent != null) {
            layer = layer.parent;
        }
        Layer newLayer = new Layer(layerable,true);
        layer.setParent(newLayer);
        layer.getLayerable().setImport(newLayer);
        return this;
    }

    @Override
    public <T> T findParentByClass(Class<T> clazz) {
        if (this.typeReference.contains(clazz)) {
            return layerable.getByClass(clazz);
        }
        T target = null;
        if (this.parent != null) {
            target = this.parent.findParentByClass(clazz);
        }
        if (target == null && !closed && this.creatable(clazz)) {
            target = layerable.getByClass(clazz);
        }
        return target;
    }

    @Override
    public <T> T findParentByName(String name) {
        if (namedReference.contains(name)) {
            return layerable.getByName(name);
        }
        T target = null;
        if (this.parent != null) {
            target = this.parent.findParentByName(name);
        }
        if (target == null && !closed) {
            target = layerable.getByName(name);
        }
        return target;
    }

    @Override
    public <T> T findParentFactory(Class clazz) {
        if (factoryReference.contains(clazz)) {
            if (layerable instanceof DependencyFactory){
                DependencyFactory factory = (DependencyFactory) layerable;
                return factory.getFactory(clazz);
            }
        }
        T target = null;
        if (this.parent != null) {
            target = this.parent.findParentFactory(clazz);
        }
        if (target == null && !closed && this.creatable(clazz)) {
            if (layerable instanceof DependencyFactory) {
                DependencyFactory factory = (DependencyFactory) layerable;
                target = factory.getFactory(clazz);
            }
        }
        return target;
    }

    @Override
    public <T> T findParentInterceptor(Class clazz) {
        if (interceptorReference.contains(clazz)) {
            if (layerable instanceof DependencyFactory) {
                DependencyFactory factory = (DependencyFactory) layerable;
                return (T)factory.getInterceptor(clazz);
            }
        }
        T target = null;
        if (this.parent != null) {
            target = this.parent.findParentInterceptor(clazz);
        }
        if (target == null && !closed && this.creatable(clazz)) {
            if (layerable instanceof DependencyFactory) {
                DependencyFactory factory = (DependencyFactory) layerable;
                target = (T) factory.getInterceptor(clazz);
            }
        }
        return target;
    }

    @Override
    public <T> List<T> findParentByAbstract(Class<T> clazz) {
        if (abstractReference.contains(clazz)) {
            return layerable.getByAbstract(clazz);
        }
        List<T> target = null;
        if (this.parent != null) {
            target = this.parent.findParentByAbstract(clazz);
        }
        if (target == null && !closed) {
            target = layerable.getByAbstract(clazz);
        }
        return target;
    }

    @Override
    public Layer getParent() {
        return this.parent;
    }

    private void setParent(Layer layer) {
        this.parent = layer;
    }

    Layerable getLayerable() {
        return layerable;
    }

    /**
     * 判断是否可以在开放层创建组件
     *
     * 如果一个类的组件信息存在于父层（父层是封闭的），那么
     * 此组件不应该被开放层创建。
     *
     * @param clazz 类
     * @return 是否允许组件的创建。
     */
    private boolean creatable(Class clazz) {
        if (parent != null && parent.contains(clazz)){
            return false;
        } else if (parent != null) {
            return parent.creatable(clazz);
        }
        return true;
    }

    /**
     * 检查可分层的Context是否存在此类的组件信息。
     * @param clazz 类
     * @return 是否存在于此可分层上下文中
     */
    private boolean contains(Class clazz) {
        return layerable.contains(clazz);
    }

    public DependencyContext asContext() {
        return new LayerDependencyContext(this);
    }

}
