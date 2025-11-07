package com.twx.iterative_methods.view;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.impl.Equation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class TwoDimPlot extends Pane {

    private final DoubleProperty xMinProp = new SimpleDoubleProperty(-1.0);
    private final DoubleProperty xMaxProp = new SimpleDoubleProperty(3.0);
    private final DoubleProperty yMinProp = new SimpleDoubleProperty(-2.0);
    private final DoubleProperty yMaxProp = new SimpleDoubleProperty(2.0);

    private Timeline panZoomAnimation;
    private Function<Double, Double> currentF, currentG;
    private final Canvas backgroundCanvas, functionCanvas, iterationCanvas;

    private List<IterationState> iterationHistory = Collections.emptyList();
    private Equation currentEquation;
    private IterativeMethod currentMethod;

    // --- [新增] 动画状态标志 ---
    private boolean isAnimating = false;

    public TwoDimPlot() {
        backgroundCanvas = new Canvas();
        functionCanvas = new Canvas();
        iterationCanvas = new Canvas();

        backgroundCanvas.widthProperty().bind(this.widthProperty());
        backgroundCanvas.heightProperty().bind(this.heightProperty());
        functionCanvas.widthProperty().bind(this.widthProperty());
        functionCanvas.heightProperty().bind(this.heightProperty());
        iterationCanvas.widthProperty().bind(this.widthProperty());
        iterationCanvas.heightProperty().bind(this.heightProperty());

        getChildren().addAll(backgroundCanvas, functionCanvas, iterationCanvas);

        xMinProp.addListener(obs -> drawAllLayers());
        xMaxProp.addListener(obs -> drawAllLayers());
        yMinProp.addListener(obs -> drawAllLayers());
        yMaxProp.addListener(obs -> drawAllLayers());
        this.widthProperty().addListener(obs -> drawAllLayers());
        this.heightProperty().addListener(obs -> drawAllLayers());
    }

    public void setPlotData(Equation equation, IterativeMethod method, List<IterationState> history) {
        this.currentEquation = equation;
        this.currentMethod = method;
        this.iterationHistory = (history != null) ? history : Collections.emptyList();
        drawAllLayers();
    }

    public void animateToNewRange(double newXMin, double newXMax, double newYMin, double newYMax, Function<Double, Double> f, Function<Double, Double> g) {
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

        // --- [修改] 在动画开始前设置标志，在结束后重置标志并进行一次最终重绘 ---
        panZoomAnimation.setOnFinished(event -> {
            isAnimating = false;
            drawAllLayers(); // 动画结束后，进行一次完整的高质量绘制
        });

        isAnimating = true; // 动画开始
        panZoomAnimation.play();
    }

    private void drawAllLayers() {
        drawBackgroundLayer();
        drawFunctionLayer();
        drawIterationLayer();
    }

    private void drawIterationLayer() {
        GraphicsContext gc = iterationCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        if (currentMethod == null || currentEquation == null || iterationHistory.isEmpty()) return;
        for (IterationState step : iterationHistory) {
            if (!Double.isNaN(step.x_k_minus_1())) {
                currentMethod.draw2DStep(gc, currentEquation, step.x_k_minus_1(), step.x_k(), this);
            }
        }
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

    private void drawGridAndTicks(GraphicsContext gc) {
        double xRange = getXMax() - getXMin();
        double yRange = getYMax() - getYMin();
        if (xRange <= 0 || yRange <= 0) return;

        double xStep = calculateNiceStep(xRange);
        double yStep = calculateNiceStep(yRange);

        // --- 1. 绘制背景网格线 ---
        gc.setStroke(Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.5));
        gc.setLineWidth(0.5);
        for (double x = Math.floor(getXMin() / xStep) * xStep; x <= getXMax(); x += xStep) {
            gc.strokeLine(mapX(x), 0, mapX(x), getHeight());
        }
        for (double y = Math.floor(getYMin() / yStep) * yStep; y <= getYMax(); y += yStep) {
            gc.strokeLine(0, mapY(y), getWidth(), mapY(y));
        }

        // --- 2. 绘制主坐标轴 (如果可见) ---
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        if (getYMin() <= 0 && getYMax() >= 0) {
            gc.strokeLine(0, mapY(0), getWidth(), mapY(0));
        }
        if (getXMin() <= 0 && getXMax() >= 0) {
            gc.strokeLine(mapX(0), 0, mapX(0), getHeight());
        }

        // --- [修改] 仅在非动画状态下绘制昂贵的刻度和文本 ---
        if (isAnimating) {
            return; // 动画期间，直接跳过后续所有昂贵的绘制
        }

        // --- 3. 绘制主坐标轴上的刻度 (如果可见) ---
        gc.setFill(Color.DARKGRAY);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);
        for (double x = Math.floor(getXMin() / xStep) * xStep; x <= getXMax(); x += xStep) {
            gc.fillText(formatNumber(x), mapX(x), mapY(0) + 5);
        }
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        for (double y = Math.floor(getYMin() / yStep) * yStep; y <= getYMax(); y += yStep) {
            if (Math.abs(y) < 1e-9) continue;
            gc.fillText(formatNumber(y), mapX(0) + 5, mapY(y));
        }

        // --- 4. 绘制浮动坐标轴 ---
        final double padding = 10.0;
        if (getXMin() > 0 || getXMax() < 0) {
            gc.setStroke(Color.GRAY);
            gc.setLineWidth(1.0);
            gc.setFill(Color.BLACK);
            gc.strokeLine(padding, 0, padding, getHeight());
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setTextBaseline(VPos.CENTER);
            for (double y = Math.floor(getYMin() / yStep) * yStep; y <= getYMax(); y += yStep) {
                double py = mapY(y);
                gc.strokeLine(padding - 4, py, padding + 4, py);
                gc.fillText(formatNumber(y), padding + 8, py);
            }
        }
        if (getYMin() > 0 || getYMax() < 0) {
            gc.setStroke(Color.GRAY);
            gc.setLineWidth(1.0);
            gc.setFill(Color.BLACK);
            double py = getHeight() - padding;
            gc.strokeLine(0, py, getWidth(), py);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.BOTTOM);
            for (double x = Math.floor(getXMin() / xStep) * xStep; x <= getXMax(); x += xStep) {
                double px = mapX(x);
                gc.strokeLine(px, py - 4, px, py + 4);
                gc.fillText(formatNumber(x), px, py - 6);
            }
        }
    }

    private void drawFunction(GraphicsContext gc, Function<Double, Double> func, Color color, double lineWidth) {
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        gc.beginPath();
        boolean firstPoint = true;

        // --- [修改] 动画期间，降低曲线绘制精度以提升性能 ---
        final double step = isAnimating ? 4.0 : 1.0; // 动画时每4个像素画一个点，平时每1个像素画一个点

        for (double px = 0; px <= getWidth(); px += step) {
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
            } catch (Exception ignored) {
                firstPoint = true;
            }
        }
        gc.stroke();
    }

    // --- 以下是未改变的辅助方法 ---
    private double calculateNiceStep(double range) {
        if (range <= 0) return 1.0;
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
    public double mapX(double x) { return (x - getXMin()) / (getXMax() - getXMin()) * getWidth(); }
    public double mapY(double y) { return (getYMax() - y) / (getYMax() - getYMin()) * getHeight(); }
    public double unmapX(double px) { return px / getWidth() * (getXMax() - getXMin()) + getXMin(); }
    public double getXMin() { return xMinProp.get(); }
    public double getXMax() { return xMaxProp.get(); }
    public double getYMin() { return yMinProp.get(); }
    public double getYMax() { return yMaxProp.get(); }
}