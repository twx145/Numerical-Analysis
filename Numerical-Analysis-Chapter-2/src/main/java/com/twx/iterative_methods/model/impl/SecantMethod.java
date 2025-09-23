package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class SecantMethod implements IterativeMethod {

    // 为双点法创建一个重载方法
    public List<Double> generateSequence(Equation equation, double x0, double x1, int iterations) {
        List<Double> sequence = new ArrayList<>();
        sequence.add(x0);
        sequence.add(x1);

        double x_prev = x0;
        double x_curr = x1;

        for (int i = 0; i < iterations; i++) {
            double fx_prev = equation.getF().apply(x_prev);
            double fx_curr = equation.getF().apply(x_curr);
            double denominator = fx_curr - fx_prev;
            if (Math.abs(denominator) < 1e-12) break;

            double x_next = x_curr - fx_curr * (x_curr - x_prev) / denominator;
            sequence.add(x_next);

            x_prev = x_curr;
            x_curr = x_next;
        }
        return sequence;
    }

    // 实现接口方法，但它实际上不会被直接调用（我们会在控制器中处理特殊情况）
    @Override
    public List<Double> generateSequence(Equation equation, double x0, int iterations) {
        // 用一个默认的 x1 调用
        return generateSequence(equation, x0, x0 + 0.1, iterations);
    }

    @Override
    public void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot) {
        double y_n = equation.getF().apply(x_n);
        double pXn_x = plot.mapX(x_n);
        double pYn_y = plot.mapY(y_n);
        double pXn1_x = plot.mapX(x_n1);
        double pZero_y = plot.mapY(0);

        gc.setStroke(Color.PURPLE);
        gc.setLineWidth(1.5);
        gc.strokeLine(pXn_x, pYn_y, pXn_x, pZero_y);
        gc.strokeLine(pXn_x, pYn_y, pXn1_x, pZero_y);
    }

    @Override
    public String getName() {
        return "双点弦截法";
    }
}