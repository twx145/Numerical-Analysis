package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class NewtonMethod implements IterativeMethod {
    @Override
    public List<Double> generateSequence(Equation equation, double x0, int iterations) {
        List<Double> sequence = new ArrayList<>();
        sequence.add(x0);
        double x = x0;
        for (int i = 0; i < iterations; i++) {
            double fx = equation.getF().apply(x);
            double dfx = equation.getDf().apply(x);
            if (Math.abs(dfx) < 1e-9) break;
            x = x - fx / dfx;
            sequence.add(x);
        }
        return sequence;
    }

    @Override
    public void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot) {
        double y_n = equation.getF().apply(x_n);

        double pXn_x = plot.mapX(x_n);
        double pYn_y = plot.mapY(y_n);
        double pXn1_x = plot.mapX(x_n1);
        double pZero_y = plot.mapY(0);

        gc.setStroke(Color.BLUE);
        gc.setLineWidth(1.5);
        gc.strokeLine(pXn_x, pYn_y, pXn_x, pZero_y); // 垂直线
        gc.strokeLine(pXn_x, pYn_y, pXn1_x, pZero_y); // 切线
    }

    @Override
    public String getName() {
        return "牛顿迭代法";
    }
}