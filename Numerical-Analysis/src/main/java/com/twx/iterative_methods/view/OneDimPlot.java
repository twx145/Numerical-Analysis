package com.twx.iterative_methods.view;

import com.twx.iterative_methods.model.IterationState;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.Collections;
import java.util.List;

/**
 * 一个数据驱动的一维绘图组件，能够动态调整视角并同步绘制所有历史迭代步骤。
 * 它继承自 Pane，以便能很好地融入JavaFX布局系统。
 */
public class OneDimPlot extends Pane {

    private final Canvas canvas;
    private final DoubleProperty xMinProp = new SimpleDoubleProperty(-1.0);
    private final DoubleProperty xMaxProp = new SimpleDoubleProperty(2.0);
    private Timeline rangeAnimation;

    // View层持有需要绘制的数据的引用
    private List<IterationState> iterationHistory = Collections.emptyList();
    private Color mappingColor = Color.RED; // 默认颜色

    public OneDimPlot() {
        canvas = new Canvas();
        getChildren().add(canvas);
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        // 监听坐标范围和尺寸的变化，自动调用主绘图方法
        xMinProp.addListener(obs -> drawPlot());
        xMaxProp.addListener(obs -> drawPlot());
        widthProperty().addListener(obs -> drawPlot());
        heightProperty().addListener(obs -> drawPlot());
    }

    /**
     * 由 Controller 调用，用于设置绘图所需的数据。
     * @param history 完整的迭代历史列表
     * @param color   用于绘制所有弧线的颜色
     */
    public void setPlotData(List<IterationState> history, Color color) {
        this.iterationHistory = (history != null) ? history : Collections.emptyList();
        this.mappingColor = color;
        // 设置新数据后立即重绘
        drawPlot();
    }

    /**
     * 启动一个平滑的动画，将视图的X轴范围调整到新的边界。
     * @param newXMin 新的X轴最小值
     * @param newXMax 新的X轴最大值
     */
    public void animateToNewRange(double newXMin, double newXMax) {
        if (rangeAnimation != null) rangeAnimation.stop();

        rangeAnimation = new Timeline(
                new KeyFrame(Duration.millis(600),
                        new KeyValue(xMinProp, newXMin, Interpolator.EASE_BOTH),
                        new KeyValue(xMaxProp, newXMax, Interpolator.EASE_BOTH)
                )
        );
        rangeAnimation.play();
    }

    /**
     * 主绘图方法，负责在一个绘制周期内绘制所有内容。
     */
    private void drawPlot() {
        GraphicsContext gc = getGC();
        gc.clearRect(0, 0, getWidth(), getHeight());
        drawAxis(gc);
        drawTicks(gc);
        drawAllMappings(gc); // 绘制所有历史弧线
    }

    /**
     * 绘制构成图表基础的坐标轴。
     */
    private void drawAxis(GraphicsContext gc) {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        double lineY = getHeight() / 2;
        gc.strokeLine(0, lineY, getWidth(), lineY);
    }

    /**
     * 绘制坐标轴上的刻度线和数字标签。
     */
    private void drawTicks(GraphicsContext gc) {
        double lineY = getHeight() / 2;
        double range = xMaxProp.get() - xMinProp.get();
        if (range <= 0 || getWidth() <= 0) return;

        double step = calculateNiceStep(range);

        gc.setFill(Color.BLACK);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);

        double startTick = Math.floor(xMinProp.get() / step) * step;
        for (double x = startTick; x <= xMaxProp.get(); x += step) {
            if (Math.abs(step) < 1e-9) break;
            double px = mapX(x);
            gc.strokeLine(px, lineY - 5, px, lineY + 5);
            gc.fillText(formatNumber(x), px, lineY + 10);
        }
    }

    /**
     * 绘制所有历史迭代的弧线。
     */
    private void drawAllMappings(GraphicsContext gc) {
        if (iterationHistory.isEmpty()) {
            return;
        }
        for (IterationState step : iterationHistory) {
            if (!Double.isNaN(step.x_k_minus_1())) {
                drawSingleMapping(gc, step.x_k_minus_1(), step.x_k(), this.mappingColor);
            }
        }
    }

    /**
     * 绘制单次迭代（从 x_n 到 x_n1）的映射弧线。
     */
    private void drawSingleMapping(GraphicsContext gc, double x_n, double x_n1, Color color) {
        double fromX = mapX(x_n);
        double toX = mapX(x_n1);
        double lineY = getHeight() / 2;
        double arcHeight = Math.max(20, Math.abs(toX - fromX) * 0.5);
        arcHeight = Math.min(arcHeight, getHeight() * 0.8);

        gc.setStroke(color);
        gc.setLineWidth(1.5);
        gc.strokeArc(Math.min(fromX, toX), lineY - arcHeight / 2, Math.abs(toX - fromX), arcHeight, 0, 180, ArcType.OPEN);

        double arrowSize = 5;
        if (toX > fromX) {
            gc.strokeLine(toX, lineY, toX - arrowSize, lineY - arrowSize);
            gc.strokeLine(toX, lineY, toX - arrowSize, lineY + arrowSize);
        } else {
            gc.strokeLine(toX, lineY, toX + arrowSize, lineY - arrowSize);
            gc.strokeLine(toX, lineY, toX + arrowSize, lineY + arrowSize);
        }
    }

    // --- 辅助方法 ---

    private GraphicsContext getGC() {
        return canvas.getGraphicsContext2D();
    }

    private double mapX(double x) {
        double padding = 30;
        double range = xMaxProp.get() - xMinProp.get();
        if (Math.abs(range) < 1e-9) return getWidth() / 2;
        return padding + (x - xMinProp.get()) / range * (getWidth() - 2 * padding);
    }

    private double calculateNiceStep(double range) {
        if (range <= 0) return 1.0;
        double roughStep = range / 6;
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
}