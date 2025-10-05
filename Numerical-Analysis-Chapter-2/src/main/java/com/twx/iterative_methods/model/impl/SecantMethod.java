package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SecantMethod implements IterativeMethod {

    // 这个方法不应该被调用，但为了接口完整性，我们提供一个默认行为
    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        // 用一个默认的 x1 调用
        return createIterator(equation, x0, x0 + 0.1);
    }

    // 这是割线法实际使用的方法
    @Override
    public MethodIterator createIterator(Equation equation, double x0, double x1) {
        return new SecantIterator(equation, x0, x1);
    }

    private static class SecantIterator implements MethodIterator {
        private final Equation equation;
        private int k = 0;
        private double x_prev;
        private double x_curr;
        private static final int MAX_ITERATIONS = 50;

        public SecantIterator(Equation equation, double x0, double x1) {
            this.equation = equation;
            this.x_prev = x0; // 对于k=0, prev是x0
            this.x_curr = x1; // 对于k=0, curr是x1, 但实际返回的是x0的信息
        }

        @Override
        public boolean hasNext() {
            return Double.isFinite(x_prev) && Double.isFinite(x_curr) && k <= MAX_ITERATIONS;
        }

        @Override
        public IterationState next() {
            // --- 特殊处理前两个点 ---
            if (k == 0) { // 返回 x0 的状态
                double fx0 = equation.getF().apply(x_prev);
                k++;
                return IterationState.initial(x_prev, fx0);
            }
            if (k == 1) { // 返回 x1 的状态
                double fx1 = equation.getF().apply(x_curr);
                double error_abs = Math.abs(x_curr - x_prev);
                IterationState state = new IterationState(k, x_curr, x_prev, fx1, error_abs, Double.NaN);
                k++;
                return state;
            }

            // --- 执行一次割线法迭代 ---
            double fx_prev = equation.getF().apply(x_prev);
            double fx_curr = equation.getF().apply(x_curr);
            double denominator = fx_curr - fx_prev;

            double x_next;
            if (Math.abs(denominator) < 1e-12) {
                x_next = Double.NaN;
            } else {
                x_next = x_curr - fx_curr * (x_curr - x_prev) / denominator;
            }

            // --- 计算日志指标 ---
            double error_abs = Math.abs(x_next - x_curr);
            double prev_error_abs = Math.abs(x_curr - x_prev);
            double error_ratio = (prev_error_abs > 1e-12) ? error_abs / prev_error_abs : Double.NaN;
            IterationState state = new IterationState(k, x_next, x_curr, equation.getF().apply(x_next), error_abs, error_ratio);

            // --- 更新状态 ---
            x_prev = x_curr;
            x_curr = x_next;
            k++;
            return state;
        }
    }

    @Override
    public void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot) {
        // 注意：割线法的绘图逻辑可能需要x_{n-1}和x_n来画割线，然后得到x_{n+1}
        // 为了简化，我们暂时保持和牛顿法类似的画法，从(x_n, f(x_n))画到(x_{n+1}, 0)
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