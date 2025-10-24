package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.function.Function;

public class AitkenMethod implements IterativeMethod {

    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        if (equation.getG() == null) {
            throw new IllegalStateException("Aitken's method requires a g(x) function to be defined.");
        }
        return new AitkenIterator(equation, x0);
    }

    private static class AitkenIterator implements MethodIterator {
        private final Equation equation;
        private int k = 0;
        // 修改：简化状态变量，逻辑更清晰
        private double x_curr; // 存储当前迭代的结果 x_k
        private double x_prev; // 存储上一次迭代的结果 x_{k-1}
        private double x_prev_prev; // 存储上上次迭代的结果 x_{k-2}
        private static final int MAX_ITERATIONS = 50;
        private final Function<Double, Double> g;

        public AitkenIterator(Equation equation, double x0) {
            this.equation = equation;
            this.g = equation.getG();
            this.x_curr = x0;
            this.x_prev = Double.NaN;
            this.x_prev_prev = Double.NaN;
        }

        @Override
        public boolean hasNext() {
            return Double.isFinite(x_curr) && k < MAX_ITERATIONS;
        }

        @Override
        public IterationState next() {
            if (k == 0) {
                double fx0 = equation.getF().apply(x_curr);
                IterationState initialState = IterationState.initial(x_curr, fx0);
                x_prev = x_curr; // 准备下一次迭代
                k++;
                return initialState;
            }

            // --- 执行一次艾特肯加速迭代 ---
            // 1. 从 x_{k-1} 开始，执行两次普通迭代，得到后续两项
            double x0_k = x_prev;
            double x1_k = g.apply(x0_k);
            double x2_k = g.apply(x1_k);

            // 2. 应用艾特肯加速公式
            double denominator = x2_k - 2 * x1_k + x0_k;
            if (Math.abs(denominator) < 1e-12) {
                x_curr = Double.NaN; // 分母为0，无法加速，迭代失败
            } else {
                x_curr = x0_k - Math.pow(x1_k - x0_k, 2) / denominator;
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

    /**
     * 修改：重写了艾特肯法的绘图逻辑以画出轨迹线。
     * 1. 首先用灰色虚线绘制出艾特肯法所加速的两次不动点迭代的“蛛网”轨迹。
     * 2. 然后用主题颜色绘制出从 (x_n,f(x_n)) 到最终加速点 (x_{n+1},0) 的路径。
     */
    @Override
    public void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot) {
        Function<Double, Double> g = equation.getG();
        if (g == null) return;

        // --- 1. 绘制作为加速基础的两次不动点迭代（蛛网图） ---
        // 计算中间步骤
        double x_intermediate_1 = g.apply(x_n);
        double x_intermediate_2 = g.apply(x_intermediate_1);

        // 映射到画布坐标
        double pXn_x = plot.mapX(x_n);
        double pXi1_x = plot.mapX(x_intermediate_1);
        double pXi1_y = plot.mapY(x_intermediate_1); // 注意这里是y=g(x)=x_intermediate_1
        double pXi2_y = plot.mapY(x_intermediate_2);

        gc.setStroke(Color.LIGHTSLATEGRAY);
        gc.setLineWidth(1.0);
        gc.setLineDashes(4, 4);

        // 蛛网轨迹:
        // (x_n, f(x_n)) -> (x_n, g(x_n)) -> (g(x_n), g(x_n)) -> (g(x_n), g(g(x_n)))
        // 为了和 y=x 辅助线关联，我们从 (x_n, y=x_n) 开始画蛛网更清晰
        gc.strokeLine(plot.mapX(x_n), plot.mapY(x_n), pXn_x, pXi1_y); // | 垂直线到 g(x)
        gc.strokeLine(pXn_x, pXi1_y, pXi1_x, pXi1_y);                 // -- 水平线到 y=x
        gc.strokeLine(pXi1_x, pXi1_y, pXi1_x, pXi2_y);                 // | 垂直线到 g(g(x))

        gc.setLineDashes(0);

        // --- 2. 绘制艾特肯加速的最终步骤 ---
        double y_n = equation.getF().apply(x_n);
        double pYn_y = plot.mapY(y_n);
        double pXn1_x = plot.mapX(x_n1);
        double pZero_y = plot.mapY(0);

        gc.setStroke(Color.DEEPPINK);
        gc.setLineWidth(1.5);

        // 从 (x_n, f(x_n)) 画一条垂直线到x轴
        gc.strokeLine(pXn_x, pYn_y, pXn_x, pZero_y);
        // 从 (x_n, 0) 画一条水平线到 (x_{n+1}, 0)，表示加速后的跳跃
        gc.strokeLine(pXn_x, pZero_y, pXn1_x, pZero_y);

        // 绘制点
        gc.setFill(Color.DEEPPINK);
        gc.fillOval(pXn_x - 3, pYn_y - 3, 6, 6);
        gc.setFill(Color.DARKVIOLET);
        gc.fillOval(pXn1_x - 3, pZero_y - 3, 6, 6);
    }

    @Override
    public String getName() {
        return "艾特肯法";
    }
}