package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;

public class DampedNewtonMethod implements IterativeMethod {

    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        return new DampedNewtonIterator(equation, x0);
    }

    private static class DampedNewtonIterator implements MethodIterator {
        private final Equation equation;
        private int k = 0;
        private double x_prev, x_curr, x_prev_prev;
        private static final int MAX_ITERATIONS = 50;

        public DampedNewtonIterator(Equation equation, double x0) {
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

            // --- 执行一次下山法迭代 ---
            double fx_prev = equation.getF().apply(x_prev);
            double dfx_prev = equation.getDf().apply(x_prev);

            if (Math.abs(dfx_prev) < 1e-12) {
                x_curr = Double.NaN; // 导数过小，无法继续
            } else {
                double lambda = 1.0;
                int maxTries = 10;
                boolean found = false;
                while (maxTries-- > 0) {
                    double x_next_candidate = x_prev - lambda * (fx_prev / dfx_prev);
                    if (Math.abs(equation.getF().apply(x_next_candidate)) < Math.abs(fx_prev)) {
                        x_curr = x_next_candidate;
                        found = true;
                        break;
                    }
                    lambda /= 2.0;
                }
                if (!found) {
                    x_curr = Double.NaN; // 未找到合适的lambda
                }
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
        // 它的绘图方式与标准牛顿法完全相同
        new NewtonMethod().draw2DStep(gc, equation, x_n, x_n1, plot);
    }

    @Override
    public String getName() {
        return "牛顿下山法";
    }
}