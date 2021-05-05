package org.swdc.dependency;

import java.util.List;

public interface Listenable<T> {

    /**
     * 添加一个listener
     * @param listener
     */
    void addListener(T listener);

    /**
     * 删除一个listener
     * @param listener
     */
    void removeListener(T listener);

    /**
     * 列出目前注册的所有listener
     * @return
     */
    List<T> getAllListeners();

}
