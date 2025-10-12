package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;

public class ModifiedSecantMethod implements IterativeMethod {
    private static final double H = 1e-6;

    /**
     * 新增：更新间隔作为一个实例变量，默认为1（即每次都更新）。
     * 这个值可以通过 setUpdateInterval() 方法从外部修改。
     */
    private int updateInterval = 1;

    /**
     * 新增：公共的 setter 方法，允许 Controller 设置更新间隔。
     * @param interval 用户从UI设置的更新频率。
     */
    public void setUpdateInterval(int interval) {
        // 确保间隔值至少为1
        this.updateInterval = Math.max(1, interval);
    }

    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        // 在创建迭代器时，将当前实例的 updateInterval 传递进去
        return new ModifiedSecantIterator(equation, x0, this.updateInterval);
    }

    private static class ModifiedSecantIterator implements MethodIterator {
        private final Equation equation;
        private int k = 0;
        private double x_prev, x_curr, x_prev_prev;
        private static final int MAX_ITERATIONS = 50;

        /**
         * 修改：不再是静态常量，而是从外部传入的实例变量。
         */
        private final int updateInterval;
        private double derivative_approx;

        /**
         * 修改：构造函数接收 updateInterval 参数。
         */
        public ModifiedSecantIterator(Equation equation, double x0, int updateInterval) {
            this.equation = equation;
            this.x_curr = x0;
            this.x_prev = Double.NaN;
            this.x_prev_prev = Double.NaN;
            this.updateInterval = updateInterval; // 保存传入的间隔值
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

            double fx = equation.getF().apply(x_prev);

            if (k == 1 || (k - 1) % this.updateInterval == 0) {
                this.derivative_approx = (equation.getF().apply(x_prev + H) - fx) / H;
            }
            // --- 执行一次修正切线法迭代 ---
            if (Math.abs(this.derivative_approx) < 1e-12) {
                x_curr = Double.NaN;
            } else {
                x_curr = x_prev - fx / this.derivative_approx;
            }

            double error_abs = Math.abs(x_curr - x_prev);
            double prev_error_abs = Math.abs(x_prev - x_prev_prev);
            double error_ratio = (prev_error_abs > 1e-12) ? error_abs / prev_error_abs : Double.NaN;
            IterationState state = new IterationState(k, x_curr, x_prev, equation.getF().apply(x_curr), error_abs, error_ratio);

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