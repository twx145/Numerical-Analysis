package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SinglePointSecantMethod implements IterativeMethod {

    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        return new SinglePointSecantIterator(equation, x0);
    }

    private static class SinglePointSecantIterator implements MethodIterator {
        private final Equation equation;
        private final double x0, fx0; // 固定的初始点
        private int k = 0;
        private double x_prev, x_curr, x_prev_prev;
        private static final int MAX_ITERATIONS = 50;

        public SinglePointSecantIterator(Equation equation, double x0) {
            this.equation = equation;
            this.x0 = x0;
            this.fx0 = equation.getF().apply(x0);
            this.x_curr = x0+1e-9;
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
                IterationState initialState = IterationState.initial(x_curr, fx0);
                x_prev = x_curr;
                k++;
                return initialState;
            }

            // --- 执行一次单点割线法迭代 ---
            double fx_prev = equation.getF().apply(x_prev);
            double denominator = fx_prev - fx0;
            if (Math.abs(denominator) < 1e-12) {
                x_curr = Double.NaN;
            } else {
                x_curr = x_prev - fx_prev * (x_prev - x0) / denominator;
            }

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
        double y_n = equation.getF().apply(x_n);
        double pXn_x = plot.mapX(x_n);
        double pYn_y = plot.mapY(y_n);
        double pXn1_x = plot.mapX(x_n1);
        double pZero_y = plot.mapY(0);
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(1.5);
        gc.strokeLine(pXn_x, pYn_y, pXn_x, pZero_y);
        gc.strokeLine(pXn_x, pYn_y, pXn1_x, pZero_y);
    }

    @Override
    public String getName() {
        return "单点弦截法";
    }
}