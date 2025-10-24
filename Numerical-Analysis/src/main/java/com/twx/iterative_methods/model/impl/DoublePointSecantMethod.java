package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class DoublePointSecantMethod implements IterativeMethod {

    // --- 核心修改：使用一个列表来存储绘图所需的所有点 ---
    // 这个列表与 MainController 的 history 是同步的，但由本类完全控制
    private final List<Double> pointsForDrawing = new ArrayList<>();

    @Override
    public MethodIterator createIterator(Equation equation, double x0) {
        throw new UnsupportedOperationException("Secant Method requires two distinct initial values, x0 and x1.");
    }

    @Override
    public MethodIterator createIterator(Equation equation, double x0, double x1) {
        // 每当开始一次新的迭代时，清空并初始化绘图点列表
        pointsForDrawing.clear();
        pointsForDrawing.add(x0);
        pointsForDrawing.add(x1);
        // 将本类的实例传递给迭代器，以便它可以回调
        return new SecantIterator(equation, x0, x1, this);
    }

    // 新增一个方法，供迭代器回调来添加新的点
    private void addPointForDrawing(double point) {
        pointsForDrawing.add(point);
    }

    private static class SecantIterator implements MethodIterator {
        private final Equation equation;
        private final DoublePointSecantMethod parent; // 用于回调
        private int k = 0;
        private double x_old, x_curr, x_older;
        private static final int MAX_ITERATIONS = 50;

        public SecantIterator(Equation equation, double x0, double x1, DoublePointSecantMethod parent) {
            this.equation = equation;
            this.parent = parent;
            this.x_older = Double.NaN;
            this.x_old = x0;
            this.x_curr = x1;
        }

        @Override
        public boolean hasNext() {
            return Double.isFinite(x_old) && Double.isFinite(x_curr) && k < MAX_ITERATIONS;
        }

        @Override
        public IterationState next() {
            if (k == 0) {
                k++;
                return IterationState.initial(x_old, equation.getF().apply(x_old));
            }
            if (k == 1) {
                double error_abs = Math.abs(x_curr - x_old);
                IterationState state = new IterationState(k, x_curr, x_old, equation.getF().apply(x_curr), error_abs, Double.NaN);
                x_older = x_old;
                x_old = x_curr;
                k++;
                return state;
            }

            double fx_old = equation.getF().apply(x_old);
            double fx_older = equation.getF().apply(x_older);
            double denominator = fx_old - fx_older;

            if (Math.abs(denominator) < 1e-12) {
                x_curr = Double.NaN;
            } else {
                x_curr = x_old - fx_old * (x_old - x_older) / denominator;
            }

            // --- 核心修改：将新计算出的点通知给父类，用于绘图 ---
            parent.addPointForDrawing(x_curr);

            double error_abs = Math.abs(x_curr - x_old);
            double prev_error_abs = Math.abs(x_old - x_older);
            double error_ratio = (prev_error_abs > 1e-12) ? error_abs / prev_error_abs : Double.NaN;
            IterationState state = new IterationState(k, x_curr, x_old, equation.getF().apply(x_curr), error_abs, error_ratio);

            x_older = x_old;
            x_old = x_curr;
            k++;
            return state;
        }
    }

    /**
     * 修改：完全重构绘图逻辑，使其无状态，并从内部列表中获取绘图所需的所有信息。
     * 它现在忽略传入的 x_n 和 x_n1 参数。
     */
    @Override
    public void draw2DStep(GraphicsContext gc, Equation equation, double x_n_ignored, double x_n1_ignored, TwoDimPlot plot) {
        int size = pointsForDrawing.size();
        // 如果点少于2个，无法绘图
        if (size < 2) return;

        // --- 初始步骤: 只画出 x0 和 x1 两个点 ---
        if (size == 2) {
            double x0 = pointsForDrawing.get(0);
            double x1 = pointsForDrawing.get(1);
            double y0 = equation.getF().apply(x0);
            double y1 = equation.getF().apply(x1);

            gc.setFill(Color.PURPLE);
            gc.fillOval(plot.mapX(x0) - 3, plot.mapY(y0) - 3, 6, 6);
            gc.setFill(Color.MEDIUMPURPLE);
            gc.fillOval(plot.mapX(x1) - 3, plot.mapY(y1) - 3, 6, 6);
            return;
        }

        // --- 常规步骤: 画出连接最近两点的割线 ---
        // 从列表中取出最后三个点
        double x_prev_prev = pointsForDrawing.get(size - 3); // x_{k-1}
        double x_prev = pointsForDrawing.get(size - 2);      // x_k
        double x_curr = pointsForDrawing.get(size - 1);      // x_{k+1}

        double y_prev_prev = equation.getF().apply(x_prev_prev);
        double y_prev = equation.getF().apply(x_prev);

        double pX_prev_prev_x = plot.mapX(x_prev_prev);
        double pY_prev_prev_y = plot.mapY(y_prev_prev);
        double pX_prev_x = plot.mapX(x_prev);
        double pY_prev_y = plot.mapY(y_prev);
        double pX_curr_x = plot.mapX(x_curr);
        double pZero_y = plot.mapY(0);

        // 1. 绘制割线 (连接 x_{k-1} 和 x_k)
        gc.setStroke(Color.PURPLE);
        gc.setLineWidth(1.5);
        gc.strokeLine(pX_prev_prev_x, pY_prev_prev_y, pX_prev_x, pY_prev_y);

        // 2. 延长割线至交点 (x_{k+1})
        gc.setLineDashes(2, 4);
        gc.strokeLine(pX_prev_x, pY_prev_y, pX_curr_x, pZero_y);
        gc.setLineDashes(0);

        // 3. 绘制点
        gc.setFill(Color.PURPLE);
        gc.fillOval(pX_prev_prev_x - 3, pY_prev_prev_y - 3, 6, 6);
        gc.fillOval(pX_prev_x - 3, pY_prev_y - 3, 6, 6);
        gc.setFill(Color.DARKMAGENTA);
        gc.fillOval(pX_curr_x - 3, pZero_y - 3, 6, 6);
    }

    @Override
    public String getName() {
        return "双点弦截法";
    }
}