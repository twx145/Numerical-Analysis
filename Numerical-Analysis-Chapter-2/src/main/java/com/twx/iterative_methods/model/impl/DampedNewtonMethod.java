package com.twx.iterative_methods.model.impl;

import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
import java.util.List;

public class DampedNewtonMethod implements IterativeMethod {
    @Override
    public List<Double> generateSequence(Equation equation, double x0, int iterations) {
        List<Double> sequence = new ArrayList<>();
        sequence.add(x0);
        double x = x0;
        for (int i = 0; i < iterations; i++) {
            double fx = equation.getF().apply(x);
            double dfx = equation.getDf().apply(x);
            if (Math.abs(dfx) < 1e-12) break;

            double lambda = 1.0;
            double x_next;
            int maxTries = 10; // 防止无限循环

            while (maxTries-- > 0) {
                x_next = x - lambda * (fx / dfx);
                if (Math.abs(equation.getF().apply(x_next)) < Math.abs(fx)) {
                    x = x_next;
                    break;
                }
                lambda /= 2.0;
            }
            if (maxTries <= 0) break; // 未找到合适的lambda

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
        return "牛顿下山法";
    }
}