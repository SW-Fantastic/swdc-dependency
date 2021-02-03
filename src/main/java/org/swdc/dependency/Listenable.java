package org.swdc.dependency;

import java.util.List;

public interface Listenable<T> {

    void addListener(T listener);

    void removeListener(T listener);

    List<T> getAllListeners();

}
