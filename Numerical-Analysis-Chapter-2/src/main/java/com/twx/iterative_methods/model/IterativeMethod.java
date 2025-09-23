package com.twx.iterative_methods.model;

import com.twx.iterative_methods.model.impl.Equation;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.scene.canvas.GraphicsContext;
import java.util.List;

public interface IterativeMethod {
    List<Double> generateSequence(Equation equation, double x0, int iterations);
    void draw2DStep(GraphicsContext gc, Equation equation, double x_n, double x_n1, TwoDimPlot plot);
    String getName();
}