package com.twx.iterative_methods.model;

import com.twx.iterative_methods.model.impl.Equation;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;

public interface IterativeMethod {
    /**
     * [重构] 创建并返回此方法的迭代器实例。
     *
     * @param equation 方程对象
     * @param x0       初始值 x0
     * @return 一个实现了 MethodIterator 接口的对象
     */
    MethodIterator createIterator(Equation equation, double x0);

    /**
     * [重构] 对于需要两个初始值的方法 (如割线法)，提供重载。
     */
    default MethodIterator createIterator(Equation equation, double x0, double x1) {
        // 默认实现抛出异常，需要此功能的子类必须覆盖它。
        throw new UnsupportedOperationException("This method does not support two initial values.");
    }

    // draw2DStep 和 getName 保持不变
    void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot);
    String getName();
}