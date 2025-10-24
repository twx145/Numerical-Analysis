package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;

public class SimplifiedNewtonMethod implements IterativeMethod {

    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        return new SimplifiedNewtonIterator(equation, x0);
    }

    private static class SimplifiedNewtonIterator implements MethodIterator {
        private final Equation equation;
        private final double dfx0; // 只计算一次的导数
        private int k = 0;
        private double x_prev, x_curr, x_prev_prev;
        private static final int MAX_ITERATIONS = 50;

        public SimplifiedNewtonIterator(Equation equation, double x0) {
            this.equation = equation;
            this.x_curr = x0;
            this.x_prev = Double.NaN;
            this.x_prev_prev = Double.NaN;
            this.dfx0 = equation.getDf().apply(x0); // 在构造时计算一次
        }

        @Override
        public boolean hasNext() {
            return Double.isFinite(x_curr) && Math.abs(dfx0) > 1e-12 && k <= MAX_ITERATIONS;
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
            // --- 执行一次简化牛顿法迭代 ---
            x_curr = x_prev - equation.getF().apply(x_prev) / dfx0;

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
        new NewtonMethod().draw2DStep(gc, equation, x_n, x_n1, plot);
    }

    @Override
    public String getName() {
        return "简化切线法";
    }
}