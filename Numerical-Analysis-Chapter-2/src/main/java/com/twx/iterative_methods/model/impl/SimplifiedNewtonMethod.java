package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class SimplifiedNewtonMethod implements IterativeMethod {
    @Override
    public List<Double> generateSequence(Equation equation, double x0, int iterations) {
        List<Double> sequence = new ArrayList<>();
        sequence.add(x0);
        double x = x0;
        double dfx0 = equation.getDf().apply(x0); // 只计算一次导数
        if (Math.abs(dfx0) < 1e-12) return sequence; // 避免除零

        for (int i = 0; i < iterations; i++) {
            x = x - equation.getF().apply(x) / dfx0;
            sequence.add(x);
        }
        return sequence;
    }

    @Override
    public void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot) {
        // 绘图与牛顿法一致
        new NewtonMethod().draw2DStep(gc, equation, x_n, x_n1, plot);
    }

    @Override
    public String getName() {
        return "简化切线法";
    }
}