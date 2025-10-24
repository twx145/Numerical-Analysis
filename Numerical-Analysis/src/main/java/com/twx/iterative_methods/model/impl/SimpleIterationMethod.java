package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SimpleIterationMethod implements IterativeMethod {

    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        return new SimpleIterationIterator(equation, x0);
    }

    private static class SimpleIterationIterator implements MethodIterator {
        private final Equation equation;
        private int k = 0;
        private double x_prev, x_curr, x_prev_prev;
        private static final int MAX_ITERATIONS = 50;

        public SimpleIterationIterator(Equation equation, double x0) {
            this.equation = equation;
            this.x_curr = x0;
            this.x_prev = Double.NaN;
            this.x_prev_prev = Double.NaN;
        }

        @Override
        public boolean hasNext() {
            return Double.isFinite(x_curr) && k <= MAX_ITERATIONS;
        }

        @Override
        public IterationState next() {
            if (k == 0) {
                double fx0 = equation.getF().apply(x_curr);
                IterationState initialState = IterationState.initial(x_curr, fx0);
                x_prev = x_curr;
                k++;
                return initialState;
            }

            // --- 执行一次普通迭代 ---
            x_curr = equation.getG().apply(x_prev);

            // --- 计算日志指标 ---
            double error_abs = Math.abs(x_curr - x_prev);
            double prev_error_abs = Math.abs(x_prev - x_prev_prev);
            double error_ratio = (prev_error_abs > 1e-12) ? error_abs / prev_error_abs : Double.NaN;
            IterationState state = new IterationState(k, x_curr, x_prev, equation.getF().apply(x_curr), error_abs, error_ratio);

            // --- 更新状态 ---
            x_prev_prev = x_prev;
            x_prev = x_curr;
            k++;
            return state;
        }
    }

    @Override
    public void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot) {
        double y_n_on_g = equation.getG().apply(x_n);
        double pXn_x = plot.mapX(x_n);
        double pXn_y_on_line = plot.mapY(x_n);
        double pYn_y_on_g = plot.mapY(y_n_on_g);
        double pXn1_x = plot.mapX(x_n1);
        gc.setStroke(Color.RED);
        gc.setLineWidth(1.5);
        // 垂直线: 从 y=x 到 y=g(x)
        gc.strokeLine(pXn_x, pXn_y_on_line, pXn_x, pYn_y_on_g);
        // 水平线: 从 (x_n, g(x_n)) 到 (x_{n+1}, g(x_n))
        gc.strokeLine(pXn_x, pYn_y_on_g, pXn1_x, pYn_y_on_g);
    }

    @Override
    public String getName() {
        return "普通迭代法";
    }
}