package com.twx.iterative_methods.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.function.Function;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Duration;
import javafx.geometry.VPos;
import javafx.scene.text.TextAlignment;
import javafx.scene.layout.Pane;

public class TwoDimPlot extends Pane {

    private final DoubleProperty xMinProp = new SimpleDoubleProperty(-1.0);
    private final DoubleProperty xMaxProp = new SimpleDoubleProperty(3.0);
    private final DoubleProperty yMinProp = new SimpleDoubleProperty(-2.0);
    private final DoubleProperty yMaxProp = new SimpleDoubleProperty(2.0);

    private Timeline panZoomAnimation;
    private Function<Double, Double> currentF, currentG;
    private final Canvas backgroundCanvas;
    private final Canvas functionCanvas;
    private final Canvas iterationCanvas;

    public TwoDimPlot() {
        backgroundCanvas = new Canvas();
        functionCanvas = new Canvas();
        iterationCanvas = new Canvas();

        // 将所有Canvas的尺寸绑定到Pane的尺寸
        backgroundCanvas.widthProperty().bind(this.widthProperty());
        backgroundCanvas.heightProperty().bind(this.heightProperty());
        functionCanvas.widthProperty().bind(this.widthProperty());
        functionCanvas.heightProperty().bind(this.heightProperty());
        iterationCanvas.widthProperty().bind(this.widthProperty());
        iterationCanvas.heightProperty().bind(this.heightProperty());

        // 按顺序添加子节点，决定了它们的层级
        getChildren().addAll(backgroundCanvas, functionCanvas, iterationCanvas);

        // 监听坐标范围变化，自动重绘背景和函数层
        xMinProp.addListener(obs -> drawBackgroundAndFunctionLayers());
        xMaxProp.addListener(obs -> drawBackgroundAndFunctionLayers());
        yMinProp.addListener(obs -> drawBackgroundAndFunctionLayers());
        yMaxProp.addListener(obs -> drawBackgroundAndFunctionLayers());
    }

    public GraphicsContext getIterationContext() {
        return iterationCanvas.getGraphicsContext2D();
    }

    public void animateToNewRange(double newXMin, double newXMax, double newYMin, double newYMax, Function<Double, Double> f, Function<Double, Double> g, Runnable onFinished) {
        this.currentF = f;
        this.currentG = g;

        if (panZoomAnimation != null) panZoomAnimation.stop();

        panZoomAnimation = new Timeline(
                new KeyFrame(Duration.millis(600),
                        new KeyValue(xMinProp, newXMin, Interpolator.EASE_BOTH),
                        new KeyValue(xMaxProp, newXMax, Interpolator.EASE_BOTH),
                        new KeyValue(yMinProp, newYMin, Interpolator.EASE_BOTH),
                        new KeyValue(yMaxProp, newYMax, Interpolator.EASE_BOTH)
                )
        );
        if (onFinished != null) {
            panZoomAnimation.setOnFinished(e -> onFinished.run());
        }
        panZoomAnimation.play();
    }

    public void drawAllLayers(Function<Double, Double> f, Function<Double, Double> g) {
        this.currentF = f;
        this.currentG = g;
        drawBackgroundAndFunctionLayers();
        clearIterations();
    }

    private void drawBackgroundAndFunctionLayers() {
        drawBackgroundLayer();
        drawFunctionLayer();
    }

    private void drawBackgroundLayer() {
        GraphicsContext gc = backgroundCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());
        drawGridAndTicks(gc);
    }

    private void drawFunctionLayer() {
        GraphicsContext gc = functionCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        if (currentF != null) drawFunction(gc, currentF, Color.BLACK, 2.0);
        if (currentG != null) drawFunction(gc, currentG, Color.CORNFLOWERBLUE, 2.0);
        drawFunction(gc, x -> x, Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.8), 1.0);
    }

    public void clearIterations() {
        iterationCanvas.getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
    }

    private void drawGridAndTicks(GraphicsContext gc) {
        double xRange = xMaxProp.get() - xMinProp.get();
        double yRange = yMaxProp.get() - yMinProp.get();
        if (xRange <= 0 || yRange <= 0) return;

        double xStep = calculateNiceStep(xRange);
        double yStep = calculateNiceStep(yRange);

        gc.setStroke(Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.5));
        gc.setLineWidth(0.5);
        gc.setFill(Color.GRAY);

        gc.setTextAlign(TextAlignment.CENTER);
        for (double x = Math.floor(xMinProp.get() / xStep) * xStep; x <= xMaxProp.get(); x += xStep) {
            double px = mapX(x);
            gc.strokeLine(px, 0, px, getHeight());
            gc.setTextBaseline(VPos.TOP);
            gc.fillText(formatNumber(x), px, mapY(0) + 5);
        }

        gc.setTextAlign(TextAlignment.LEFT);
        for (double y = Math.floor(yMinProp.get() / yStep) * yStep; y <= yMaxProp.get(); y += yStep) {
            double py = mapY(y);
            gc.strokeLine(0, py, getWidth(), py);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(formatNumber(y), mapX(0) + 5, py);
        }

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeLine(0, mapY(0), getWidth(), mapY(0));
        gc.strokeLine(mapX(0), 0, mapX(0), getHeight());
    }

    private double calculateNiceStep(double range) {
        double roughStep = range / 8;
        double exponent = Math.floor(Math.log10(roughStep));
        double powerOf10 = Math.pow(10, exponent);
        double fraction = roughStep / powerOf10;

        if (fraction < 1.5) return powerOf10;
        if (fraction < 3.5) return 2 * powerOf10;
        if (fraction < 7.5) return 5 * powerOf10;
        return 10 * powerOf10;
    }

    private String formatNumber(double num) {
        if (Math.abs(num) < 1e-9) return "0";
        return String.format("%.2g", num);
    }

    public double mapX(double x) { return (x - xMinProp.get()) / (xMaxProp.get() - xMinProp.get()) * getWidth(); }
    public double mapY(double y) { return (yMaxProp.get() - y) / (yMaxProp.get() - yMinProp.get()) * getHeight(); }
    public double unmapX(double px) { return px / getWidth() * (xMaxProp.get() - xMinProp.get()) + xMinProp.get(); }

    private void drawFunction(GraphicsContext gc, Function<Double, Double> func, Color color, double lineWidth) {
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        gc.beginPath();
        boolean firstPoint = true;
        for (double px = 0; px <= getWidth(); px++) {
            double x = unmapX(px);
            try {
                double y = func.apply(x);
                if (Double.isFinite(y)) {
                    double py = mapY(y);
                    if (firstPoint) {
                        gc.moveTo(px, py);
                        firstPoint = false;
                    } else {
                        gc.lineTo(px, py);
                    }
                } else {
                    firstPoint = true;
                }
            } catch (Exception ignored) { firstPoint = true; }
        }
        gc.stroke();
    }

    public double getXMin() { return xMinProp.get(); }
    public double getXMax() { return xMaxProp.get(); }
}