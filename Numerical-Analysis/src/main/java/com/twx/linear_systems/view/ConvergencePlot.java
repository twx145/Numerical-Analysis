// 文件路径: src/com/twx/linear_systems/view/ConvergencePlot.java
package com.twx.linear_systems.view;

import com.twx.linear_systems.model.VectorIterationState;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class ConvergencePlot extends LineChart<Number, Number> {
    private final XYChart.Series<Number, Number> series;

    public ConvergencePlot() {
        super(new NumberAxis(), new NumberAxis());
        ((NumberAxis) getXAxis()).setLabel("迭代次数 (k)");
        ((NumberAxis) getYAxis()).setLabel("残差范数 ||Ax-b||");
        this.setTitle("迭代收敛过程");
        this.setAnimated(true);

        series = new XYChart.Series<>();
        series.setName("残差");
        this.getData().add(series);
    }

    public void addState(VectorIterationState state) {
        series.getData().add(new XYChart.Data<>(state.k(), state.residualNorm()));
    }

    public void clear() {
        series.getData().clear();
    }
}