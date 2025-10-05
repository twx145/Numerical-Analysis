package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;

public class ModifiedSecantMethod implements IterativeMethod {
    private static final double H = 1e-6;

    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        return new ModifiedSecantIterator(equation, x0);
    }

    private static class ModifiedSecantIterator implements MethodIterator {
        private final Equation equation;
        private int k = 0;
        private double x_prev, x_curr, x_prev_prev;
        private static final int MAX_ITERATIONS = 50;

        public ModifiedSecantIterator(Equation equation, double x0) {
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

            // --- 执行一次修正切线法迭代 ---
            double fx = equation.getF().apply(x_prev);
            double derivative_approx = (equation.getF().apply(x_prev + H) - fx) / H;
            if (Math.abs(derivative_approx) < 1e-12) {
                x_curr = Double.NaN;
            } else {
                x_curr = x_prev - fx / derivative_approx;
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
        new NewtonMethod().draw2DStep(gc, equation, x_n, x_n1, plot);
    }

    @Override
    public String getName() {
        return "修正切线法";
    }
}