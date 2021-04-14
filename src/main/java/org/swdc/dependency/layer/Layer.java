package org.swdc.dependency.layer;

import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.DependencyFactory;
import org.swdc.dependency.registry.ComponentInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认依赖层的实现。
 */
public class Layer implements DependencyLayer {

    /**
     * root会存储其他的layer
     */
    private List<Layer> layers;

    /**
     * 父组件
     */
    private Layer root;

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
        this.layers = new ArrayList<>();
        this.layerable = (Layerable) layerable;
        this.layerable.setImport(this);
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
        this.layers = new ArrayList<>();
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

        Layer newLayer = new Layer(layerable,true);
        newLayer.setRoot(this);
        newLayer.getLayerable().setImport(newLayer);
        this.layers.add(newLayer);
        return this;
    }

    @Override
    public <T> T findLayersByClass(Class<T> clazz,Layerable from) {
        if (root != null) {
            return root.findLayersByClass(clazz,from);
        }
        T target = null;
        for (Layer layer: layers) {
            if (layer.getLayerable() == from) {
                continue;
            }
            if (layer.typeReference.contains(clazz)) {
                return layer.getLayerable().getByClass(clazz);
            }
        }
        if (!closed && this.creatable(clazz) && this.layerable != from) {
            target = layerable.getByClass(clazz);
        }
        return target;
    }

    @Override
    public <T> T findLayersByName(String name,Layerable layerable) {
        if (root != null) {
            return root.findLayersByName(name,layerable);
        }
        for (Layer layer: layers) {
            if (layer.getLayerable() == layerable) {
                continue;
            }
            if (layer.namedReference.contains(name)) {
                return layer.getLayerable().getByName(name);
            }
        }
        return null;
    }

    @Override
    public <T> T findLayersFactory(Class clazz,Layerable layerable) {
        if (root != null) {
            return root.findLayersFactory(clazz,layerable);
        }
        T target = null;
        for (Layer layer: layers) {
            if (layer.factoryReference.contains(clazz)) {
                Layerable layerableItem = layer.getLayerable();
                if (layerableItem == layerable) {
                    continue;
                }
                if (layerableItem instanceof DependencyFactory){
                    DependencyFactory factory = (DependencyFactory) layerableItem;
                    return factory.getFactory(clazz);
                }
            }
        }
        if (!closed && this.creatable(clazz) && layerable != this.layerable) {
            if (layerable instanceof DependencyFactory) {
                DependencyFactory factory = (DependencyFactory) layerable;
                target = factory.getFactory(clazz);
            }
        }
        return target;
    }

    @Override
    public <T> T findLayersInterceptor(Class clazz,Layerable from) {
        if (root != null) {
            return root.findLayersInterceptor(clazz,from);
        }
        for (Layer layer: layers) {
            Layerable layerable = layer.getLayerable();
            if (layerable == from) {
                continue;
            }
            if (layer.interceptorReference.contains(clazz) && (layerable instanceof DependencyFactory)) {
                DependencyFactory factory = (DependencyFactory) layerable;
                return (T)factory.getInterceptor(clazz);
            }
        }
        T target = null;
        if (!closed && this.creatable(clazz) && this.layerable != from) {
            if (layerable instanceof DependencyFactory) {
                DependencyFactory factory = (DependencyFactory) layerable;
                target = (T) factory.getInterceptor(clazz);
            }
        }
        return target;
    }

    @Override
    public <T> List<T> findLayersByAbstract(Class<T> clazz,Layerable from) {
        if (root !=  null) {
            return  root.findLayersByAbstract(clazz,from);
        }
        for (Layer layer: layers) {
            if (layer.getLayerable() == from) {
                continue;
            }
            if (layer.abstractReference.contains(clazz)) {
                return layer.getLayerable().getByAbstract(clazz);
            }
        }
        List<T> target = null;
        if (!closed && this.layerable != from) {
            target = layerable.getByAbstract(clazz);
        }
        return target;
    }


    private void setRoot(Layer layer) {
        this.root = layer;
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
        if (root != null) {
            return root.creatable(clazz);
        }
        for (Layer layer: layers) {
            if(layer.contains(clazz)) {
                return false;
            }
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
