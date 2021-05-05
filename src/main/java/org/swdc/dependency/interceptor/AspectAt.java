package org.swdc.dependency.interceptor;

public enum AspectAt {

    /**
     * 于方法执行前执行此横切方法
     */
    BEFORE,
    /**
     * 于方法执行后执行此横切方法
     */
    AFTER,
    /**
     * 环绕方法执行此横切方法
     */
    AROUND,
    /**
     * 方法返回后执行此横切方法
     */
    AFTER_RETURNING,
    /**
     * 方法抛出异常后执行此横切方法
     */
    AFTER_THROWING

}
