package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SinglePointSecantMethod implements IterativeMethod {

    // 用于在绘图时能够访问到固定点x0
    private double fixedPointForDrawing;

    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        throw new UnsupportedOperationException("Single-Point Secant Method requires two initial values: a fixed point x0 and a starting point x1.");
    }

    @Override
    public MethodIterator createIterator(Equation equation, double x0, double x1) {
        // 将固定点保存起来，以便绘图时使用
        this.fixedPointForDrawing = x0;
        return new SinglePointSecantIterator(equation, x0, x1);
    }

    private static class SinglePointSecantIterator implements MethodIterator {
        private final Equation equation;
        private final double x_fixed, fx_fixed; // 固定的初始点及其函数值
        private int k = 1; // 迭代从 k=1 (即x1) 开始
        private double x_curr, x_prev, x_prev_prev;
        private static final int MAX_ITERATIONS = 50;

        public SinglePointSecantIterator(Equation equation, double x0, double x1) {
            this.equation = equation;
            this.x_fixed = x0; // 保存固定点
            this.fx_fixed = equation.getF().apply(x0); // 保存固定点的函数值

            this.x_curr = x1;     // 当前点是第一个活动点 x1
            this.x_prev = x0;     // x1 的前一个点是 x0
            this.x_prev_prev = Double.NaN;
        }

        @Override
        public boolean hasNext() {
            return Double.isFinite(x_curr) && k < MAX_ITERATIONS;
        }

        @Override
        public IterationState next() {
            // --- 首次调用 (k=1) ---
            if (k == 1) {
                double error_abs = Math.abs(x_curr - x_prev);
                IterationState firstState = new IterationState(k, x_curr, x_prev, equation.getF().apply(x_curr), error_abs, Double.NaN);
                x_prev_prev = x_prev;
                x_prev = x_curr;
                k++;
                return firstState;
            }

            // --- 后续调用 (k > 1)，执行一次单点割线法迭代 ---
            double fx_prev = equation.getF().apply(x_prev);
            double denominator = fx_prev - fx_fixed;

            if (Math.abs(denominator) < 1e-12) {
                x_curr = Double.NaN; // 迭代失败
            } else {
                x_curr = x_prev - fx_prev * (x_prev - x_fixed) / denominator;
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

    /**
     * 修改：增加了对初始步骤的特殊处理。
     * 当 x_n 是固定点时，我们只画出两个初始点，不画连线。
     */
    @Override
    public void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot) {
        double x_fixed = this.fixedPointForDrawing;

        // --- 关键修改：处理初始的可视化步骤 ---
        // 如果上一个点 (x_n) 就是固定点，说明这是第一步，我们只画出初始的两个点。
        if (Math.abs(x_n - x_fixed) < 1e-12) {
            double y_fixed = equation.getF().apply(x_fixed);
            double y_n1 = equation.getF().apply(x_n1); // y_n1 就是 f(x1)

            double pXfixed_x = plot.mapX(x_fixed);
            double pYfixed_y = plot.mapY(y_fixed);
            double pXn1_x = plot.mapX(x_n1);
            double pYn1_y = plot.mapY(y_n1);

            // 绘制固定点 (x0, f(x0))
            gc.setFill(Color.DARKGOLDENROD);
            gc.fillOval(pXfixed_x - 4, pYfixed_y - 4, 8, 8);
            // 绘制第一个活动点 (x1, f(x1))
            gc.setFill(Color.ORANGE);
            gc.fillOval(pXn1_x - 3, pYn1_y - 3, 6, 6);
            return; // 直接返回，不执行下面的连线逻辑
        }

        // --- 对于后续的所有常规步骤，执行正常的绘图逻辑 ---
        double y_fixed = equation.getF().apply(x_fixed);
        double y_n = equation.getF().apply(x_n);

        // 将所有点映射到画布坐标
        double pXfixed_x = plot.mapX(x_fixed);
        double pYfixed_y = plot.mapY(y_fixed);
        double pXn_x = plot.mapX(x_n);
        double pYn_y = plot.mapY(y_n);
        double pXn1_x = plot.mapX(x_n1);
        double pZero_y = plot.mapY(0);

        // 1. 绘制核心的割线：连接 (x_n, y_n) 和固定的 (x_fixed, y_fixed)
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(1.5);
        gc.strokeLine(pXfixed_x, pYfixed_y, pXn_x, pYn_y);

        // 2. 将割线延长至与x轴的交点 (x_{n+1}, 0)
        gc.setLineDashes(2, 4);
        gc.strokeLine(pXn_x, pYn_y, pXn1_x, pZero_y);
        gc.setLineDashes(0);

        // 3. 绘制点
        gc.setFill(Color.DARKGOLDENROD); // 固定点
        gc.fillOval(pXfixed_x - 4, pYfixed_y - 4, 8, 8);
        gc.setFill(Color.ORANGE); // 当前活动点
        gc.fillOval(pXn_x - 3, pYn_y - 3, 6, 6);
        gc.setFill(Color.DARKORANGE); // 新产生的点
        gc.fillOval(pXn1_x - 3, pZero_y - 3, 6, 6);
    }

    @Override
    public String getName() {
        return "单点弦截法";
    }
}