package com.twx.iterative_methods.model;

/**
 * 迭代器接口，取代了原有的 generateSequence 方法。
 * 允许控制器按需、逐一地获取迭代结果。
 */
public interface MethodIterator {
    /**
     * @return 如果还有下一次迭代，则返回 true
     */
    boolean hasNext();

    /**
     * 计算并返回下一次的迭代状态。
     * @return 包含该步骤所有信息的 IterationState 对象。
     */
    IterationState next();
}