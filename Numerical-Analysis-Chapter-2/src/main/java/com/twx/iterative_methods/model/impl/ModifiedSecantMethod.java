package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class ModifiedSecantMethod implements IterativeMethod {
    private static final double H = 1e-6; // 小步长

    @Override
    public List<Double> generateSequence(Equation equation, double x0, int iterations) {
        List<Double> sequence = new ArrayList<>();
        sequence.add(x0);
        double x = x0;
        for (int i = 0; i < iterations; i++) {
            double fx = equation.getF().apply(x);
            double derivative_approx = (equation.getF().apply(x + H) - fx) / H;
            if (Math.abs(derivative_approx) < 1e-12) break;
            x = x - fx / derivative_approx;
            sequence.add(x);
        }
        return sequence;
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