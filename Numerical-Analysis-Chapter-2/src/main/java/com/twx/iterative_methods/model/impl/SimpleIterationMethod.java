package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.impl.Equation;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class SimpleIterationMethod implements IterativeMethod {
    @Override
    public List<Double> generateSequence(Equation equation, double x0, int iterations) {
        List<Double> sequence = new ArrayList<>();
        sequence.add(x0);
        double x = x0;
        for (int i = 0; i < iterations; i++) {
            x = equation.getG().apply(x);
            sequence.add(x);
        }
        return sequence;
    }

    @Override
    public void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot) {
        double y_n = equation.getG().apply(x_n);

        double pXn_x = plot.mapX(x_n);
        double pXn_y = plot.mapY(x_n);
        double pYn_y = plot.mapY(y_n);
        double pXn1_x = plot.mapX(x_n1);

        gc.setStroke(Color.RED);
        gc.setLineWidth(1.5);
        gc.strokeLine(pXn_x, pXn_y, pXn_x, pYn_y);
        gc.strokeLine(pXn_x, pYn_y, pXn1_x, pYn_y);
    }

    @Override
    public String getName() {
        return "普通迭代法";
    }
}