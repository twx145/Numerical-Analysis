package com.twx.iterative_methods.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;

public class OneDimPlot extends Canvas {

    // 静态范围，因为1D图通常关注一个特定区域
    private final double xMin = -1.0;
    private final double xMax = 2.0;

    private Timeline axisAnimation;

    public OneDimPlot() {
        // 监听尺寸变化以重绘
        widthProperty().addListener(evt -> drawBase());
        heightProperty().addListener(evt -> drawBase());
    }

    /**
     * 带有动画效果的基础绘制入口。
     */
    public void animateAndDrawBase() {
        if (axisAnimation != null && axisAnimation.getStatus() == Animation.Status.RUNNING) {
            axisAnimation.stop();
        }

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        DoubleProperty progress = new SimpleDoubleProperty(0);
        progress.addListener((obs, oldVal, newVal) -> {
            drawAnimatedAxis(gc, newVal.doubleValue());
        });

        axisAnimation = new Timeline(
                new KeyFrame(Duration.millis(600), new KeyValue(progress, 1, Interpolator.EASE_BOTH))
        );
        // 动画结束后绘制刻度
        axisAnimation.setOnFinished(event -> drawTicks(gc));
        axisAnimation.play();
    }

    /**
     * 无动画的静态绘制。
     */
    public void drawBase() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        drawAnimatedAxis(gc, 1.0); // progress = 1 表示立即画完
        drawTicks(gc);
    }

    /**
     * 清除所有迭代路径（弧线）。
     */
    public void clearMappings() {
        // 由于没有分层，我们通过重绘基础来“清除”
        drawBase();
    }

    private void drawAnimatedAxis(GraphicsContext gc, double progress) {
        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        double lineY = getHeight() / 2;
        double centerX = getWidth() / 2;

        // 从中心向两边画出数轴
        gc.strokeLine(centerX, lineY, centerX + centerX * progress, lineY);
        gc.strokeLine(centerX, lineY, centerX - centerX * progress, lineY);
    }

    private void drawTicks(GraphicsContext gc) {
        double lineY = getHeight() / 2;
        double range = xMax - xMin;
        double step = range / 6; // 分成6段

        gc.setFill(Color.BLACK);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);

        for (int i = 0; i <= 6; i++) {
            double x = xMin + i * step;
            double px = mapX(x);
            gc.strokeLine(px, lineY - 5, px, lineY + 5);
            gc.fillText(String.format("%.2f", x), px, lineY + 10);
        }
    }

    /**
     * 绘制从 x_n 到 x_n1 的映射弧线。
     * @param x_n  起始点
     * @param x_n1 终点
     * @param color 弧线颜色
     */
    public void drawMapping(double x_n, double x_n1, Color color) {
        GraphicsContext gc = getGraphicsContext2D();
        double fromX = mapX(x_n);
        double toX = mapX(x_n1);
        double lineY = getHeight() / 2;
        double arcHeight = 30 + Math.abs(toX - fromX) * 0.15;

        gc.setStroke(color);
        gc.setLineWidth(1.5);

        // 绘制弧线
        gc.strokeArc(Math.min(fromX, toX), lineY - arcHeight / 2, Math.abs(toX - fromX), arcHeight, 0, 180, ArcType.OPEN);

        // 绘制终点箭头
        double arrowSize = 5;
        if (toX > fromX) {
            gc.strokeLine(toX, lineY, toX - arrowSize, lineY - arrowSize);
            gc.strokeLine(toX, lineY, toX - arrowSize, lineY + arrowSize);
        } else {
            gc.strokeLine(toX, lineY, toX + arrowSize, lineY - arrowSize);
            gc.strokeLine(toX, lineY, toX + arrowSize, lineY + arrowSize);
        }
    }

    private double mapX(double x) {
        double padding = 30; // 给两边留出空间
        return padding + (x - xMin) / (xMax - xMin) * (getWidth() - 2 * padding);
    }
}