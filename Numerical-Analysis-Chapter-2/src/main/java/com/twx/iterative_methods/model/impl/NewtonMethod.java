package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class NewtonMethod implements IterativeMethod {

    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        return new NewtonIterator(equation, x0);
    }

    private static class NewtonIterator implements MethodIterator {
        private final Equation equation;
        private int k = 0;
        private double x_prev, x_curr, x_prev_prev;
        private static final int MAX_ITERATIONS = 50; // 防止无限循环

        public NewtonIterator(Equation equation, double x0) {
            this.equation = equation;
            this.x_curr = x0;
            this.x_prev = Double.NaN;
            this.x_prev_prev = Double.NaN;
        }

        @Override
        public boolean hasNext() {
            // 只要当前解有效且未达到最大迭代次数，就继续
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
            // --- 执行一次牛顿法迭代 ---
            double fx = equation.getF().apply(x_prev);
            double dfx = equation.getDf().apply(x_prev);

            if (Math.abs(dfx) < 1e-12) {
                x_curr = Double.NaN;
            } else {
                x_curr = x_prev - fx / dfx;
            }

            // --- 计算所有日志指标 ---
            double error_abs = Math.abs(x_curr - x_prev);
            double prev_error_abs = Math.abs(x_prev - x_prev_prev);
            double error_ratio = (prev_error_abs > 1e-12) ? error_abs / prev_error_abs : Double.NaN;

            IterationState state = new IterationState(k, x_curr, x_prev, equation.getF().apply(x_curr), error_abs, error_ratio);

            // --- 更新状态为下一次迭代做准备 ---
            x_prev_prev = x_prev;
            x_prev = x_curr;
            k++;

            return state;
        }
    }

    // draw2DStep 和 getName 方法保持不变
    @Override
    public void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot) {
        double y_n = equation.getF().apply(x_n);
        double pXn_x = plot.mapX(x_n);
        double pYn_y = plot.mapY(y_n);
        double pXn1_x = plot.mapX(x_n1);
        double pZero_y = plot.mapY(0);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(1.5);
        gc.strokeLine(pXn_x, pYn_y, pXn_x, pZero_y);
        gc.strokeLine(pXn_x, pYn_y, pXn1_x, pZero_y);
    }

    @Override
    public String getName() {
        return "牛顿迭代法";
    }
}