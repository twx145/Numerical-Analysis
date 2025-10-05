package com.twx.iterative_methods.controller;

import com.twx.iterative_methods.model.IterationState;
import com.twx.iterative_methods.model.IterativeMethod;
import com.twx.iterative_methods.model.MethodIterator;
import com.twx.iterative_methods.model.impl.*;
import com.twx.iterative_methods.view.OneDimPlot;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class MainController {

    public VBox plotContainer;
    // --- FXML Controls ---
    @FXML private TextField fField, gField, initialValueField, secondInitialValueField;
    @FXML private Label gLabel;
    @FXML private ComboBox<String> methodComboBox;
    @FXML private Button resetButton, nextStepButton, clearButton;
    @FXML private TwoDimPlot twoDimPlot;
    @FXML private OneDimPlot oneDimPlot;
    @FXML private HBox x1Container;
    @FXML private TextArea logArea;

    // --- 状态管理变量 ---
    private Equation currentEquation;
    private MethodIterator currentIterator;
    private final List<IterationState> iterationHistory = new ArrayList<>();
    private final IterativeMethod[] methods = {
            new SimpleIterationMethod(), new NewtonMethod(), new SteffensenMethod(),
            new SimplifiedNewtonMethod(), new ModifiedSecantMethod(), new DampedNewtonMethod(),
            new SinglePointSecantMethod(), new SecantMethod()
    };
    private final String simpleIterationName = new SimpleIterationMethod().getName();
    private final String secantMethodName = new SecantMethod().getName();

    @FXML
    public void initialize() {
        methodComboBox.setItems(FXCollections.observableArrayList(
                Stream.of(methods).map(IterativeMethod::getName).toList()
        ));
        methodComboBox.getSelectionModel().select(1); // Default to Newton's method

        methodComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateUiForSelectedMethod());
        updateUiForSelectedMethod();

        resetButton.setOnAction(e -> initializeIteration());
        nextStepButton.setOnAction(e -> performNextStep());
        clearButton.setOnAction(e -> clearAll());
    }

    /**
     * "开始/重置" 按钮的事件处理程序。
     * 验证输入、创建迭代器、将数据传递给视图，并执行第0步。
     */
    private void initializeIteration() {
        try {
            // 1. 清理工作
            clearAll();
            iterationHistory.clear();

            // 2. 解析和验证输入
            String gStr = methodComboBox.getSelectionModel().getSelectedItem().equals(simpleIterationName) ? gField.getText() : "";
            currentEquation = new Equation(fField.getText(), gStr);
            double x0 = Double.parseDouble(initialValueField.getText());

            // 3. 将绘图所需数据传递给View
            IterativeMethod selectedMethod = methods[methodComboBox.getSelectionModel().getSelectedIndex()];
            twoDimPlot.setPlotData(currentEquation, selectedMethod, iterationHistory);
            oneDimPlot.setPlotData(iterationHistory, getColorForMethod(selectedMethod));

            // 4. 创建迭代器
            if (selectedMethod.getName().equals(secantMethodName)) {
                double x1 = Double.parseDouble(secondInitialValueField.getText());
                currentIterator = selectedMethod.createIterator(currentEquation, x0, x1);
            } else {
                currentIterator = selectedMethod.createIterator(currentEquation, x0);
            }

            // 5. 执行并记录第0步 (初始状态)
            if (currentIterator.hasNext()) {
                IterationState initialState = currentIterator.next();
                iterationHistory.add(initialState);
                logInitialState(initialState);
            } else {
                showError("Initialization Error", "Could not create iterator. Check function and initial value.");
                return;
            }

            // 6. 准备UI
            // 视图会根据新数据自动重绘，不需要手动调用 drawBase
            drawFunctionWithInitialBounds(x0);
            nextStepButton.setDisable(false);

        } catch (Exception e) {
            showError("Input Invalid", "Please check function expressions or parameters.\nError: " + e.getMessage());
            nextStepButton.setDisable(true);
        }
    }

    /**
     * "下一步" 按钮的事件处理程序。
     * 执行一次迭代，更新日志，并触发两个视图的动画。
     */
    private void performNextStep() {
        if (currentIterator == null || !currentIterator.hasNext()) {
            nextStepButton.setDisable(true);
            return;
        }

        IterationState newState = currentIterator.next();
        iterationHistory.add(newState);
        logIterationStep(newState);

        // [关键修改] 获取当前选择的方法，并将其传递给聚焦逻辑
        IterativeMethod selectedMethod = methods[methodComboBox.getSelectionModel().getSelectedIndex()];

        double[] newBounds2D = calculateDynamicBounds2D(newState, selectedMethod);
        twoDimPlot.animateToNewRange(newBounds2D[0], newBounds2D[1], newBounds2D[2], newBounds2D[3],
                currentEquation.getF(), currentEquation.getG());

        double[] newBounds1D = calculateDynamicBounds1D(newState);
        oneDimPlot.animateToNewRange(newBounds1D[0], newBounds1D[1]);

        if (!currentIterator.hasNext() || Math.abs(newState.fx_k()) < 1e-12 || newState.error_abs() < 1e-12) {
            nextStepButton.setDisable(true);
        }
    }

    /**
     * 清理所有绘图、日志和状态。
     */
    private void clearAll() {
        currentIterator = null;
        iterationHistory.clear();

        if (twoDimPlot != null) {
            twoDimPlot.setPlotData(null, null, null);
        }
        if (oneDimPlot != null) {
            // 传递一个空列表来清空
            oneDimPlot.setPlotData(Collections.emptyList(), Color.BLACK);
        }
        if (logArea != null) {
            logArea.clear();
        }
        if (nextStepButton != null) {
            nextStepButton.setDisable(true);
        }
    }

    // --- 日志记录辅助方法 ---

    private void logInitialState(IterationState state) {
        logArea.setText(String.format("%-4s | %-18s | %-18s | %-18s | %-18s\n",
                "k", "x_k", "f(x_k)", "|x_k - x_{k-1}|", "Ratio"));
        logArea.appendText("-".repeat(85) + "\n");
        logArea.appendText(String.format("%-4d | %-18.12f | %-18.12f | %-18s | %-18s\n",
                state.k(), state.x_k(), state.fx_k(), "N/A", "N/A"));
    }

    private void logIterationStep(IterationState state) {
        logArea.appendText(String.format("%-4d | %-18.12f | %-18.12f | %-18.12e | %-18.12f\n",
                state.k(), state.x_k(), state.fx_k(), state.error_abs(), state.error_ratio()));
    }

    // --- 绘图与动画辅助方法 ---

    private void drawFunctionWithInitialBounds(double x0) {
        double range = 5.0;
        double y_at_x0;
        try {
            y_at_x0 = currentEquation.getF().apply(x0);
        } catch (Exception e) {
            y_at_x0 = 0.0; // 如果初始点函数值无效，则以0为中心
        }

        twoDimPlot.animateToNewRange(x0 - range/2, x0 + range/2, y_at_x0 - range/2, y_at_x0 + range/2,
                currentEquation.getF(), currentEquation.getG());
    }

    /**
     * [关键修改] 此方法现在接受一个 IterativeMethod 参数，并根据方法类型选择不同的聚焦策略。
     * @param state  当前的迭代状态
     * @param method 当前使用的迭代方法
     * @return 一个包含 [minX, maxX, minY, maxY] 的数组
     */
    private double[] calculateDynamicBounds2D(IterationState state, IterativeMethod method) {
        // --- 1. 计算X轴范围 (所有方法通用) ---
        double x1 = state.x_k_minus_1();
        double x2 = state.x_k();
        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double paddingX = Math.max((maxX - minX) * 0.5, 0.01);
        double finalMinX = minX - paddingX;
        double finalMaxX = maxX + paddingX;

        // --- 2. 根据方法类型计算Y轴范围 ---
        double minY, maxY;

        if (method instanceof SimpleIterationMethod) {
            // --- 策略 A: 针对普通迭代法 ---
            // 目标是聚焦于 y=g(x) 和 y=x 的交点
            minY = Double.POSITIVE_INFINITY;
            maxY = Double.NEGATIVE_INFINITY;

            // 在X范围内采样 g(x) 函数以找到其局部极值
            int samples = 100;
            for (int i = 0; i <= samples; i++) {
                double x = finalMinX + i * (finalMaxX - finalMinX) / samples;
                try {
                    double valG = currentEquation.getG().apply(x);
                    if (Double.isFinite(valG)) {
                        minY = Math.min(minY, valG);
                        maxY = Math.max(maxY, valG);
                    }
                } catch (Exception ignored) {}
            }

            // 确保 y=x 这条线在视图范围内
            minY = Math.min(minY, finalMinX);
            maxY = Math.max(maxY, finalMaxX);

        } else {
            // --- 策略 B: 针对所有其他求根方法 (牛顿法等) ---
            // 目标是聚焦于 f(x) 和 x轴 (y=0) 的交点
            minY = Double.POSITIVE_INFINITY;
            maxY = Double.NEGATIVE_INFINITY;

            // 在X范围内采样 f(x) 函数
            int samples = 100;
            for (int i = 0; i <= samples; i++) {
                double x = finalMinX + i * (finalMaxX - finalMinX) / samples;
                try {
                    double valF = currentEquation.getF().apply(x);
                    if (Double.isFinite(valF)) {
                        minY = Math.min(minY, valF);
                        maxY = Math.max(maxY, valF);
                    }
                } catch (Exception ignored) {}
            }

            // 确保 x轴 (y=0) 在视图范围内
            minY = Math.min(minY, 0);
            maxY = Math.max(maxY, 0);
        }

        // --- 3. 添加Y轴边距并返回 (所有方法通用) ---
        if (!Double.isFinite(minY) || !Double.isFinite(maxY)){
            minY = -5; maxY = 5; // Fallback
        }
        double paddingY = Math.max((maxY - minY) * 0.2, 0.01);
        return new double[]{finalMinX, finalMaxX, minY - paddingY, maxY + paddingY};
    }

    private double[] calculateDynamicBounds1D(IterationState state) {
        double x1 = state.x_k_minus_1();
        double x2 = state.x_k();

        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double paddingX = Math.max((maxX - minX) * 1.5, 0.01);

        return new double[]{minX - paddingX, maxX + paddingX};
    }

    // --- UI辅助方法 ---

    private void updateUiForSelectedMethod() {
        String selected = methodComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        boolean isSimple = selected.equals(simpleIterationName);
        gLabel.setVisible(isSimple); gField.setVisible(isSimple);
        gLabel.setManaged(isSimple); gField.setManaged(isSimple);
        boolean isSecant = selected.equals(secantMethodName);
        x1Container.setVisible(isSecant); x1Container.setManaged(isSecant);
    }

    private Color getColorForMethod(IterativeMethod method) {
        if (method instanceof NewtonMethod) return Color.BLUE;
        if (method instanceof SteffensenMethod) return Color.GREEN;
        if (method instanceof SimplifiedNewtonMethod) return Color.rgb(0, 100, 100);
        if (method instanceof ModifiedSecantMethod) return Color.rgb(255, 140, 0);
        if (method instanceof DampedNewtonMethod) return Color.rgb(75, 0, 130);
        if (method instanceof SinglePointSecantMethod) return Color.ORANGE;
        if (method instanceof SecantMethod) return Color.PURPLE;
        return Color.RED; // Default for SimpleIterationMethod etc.
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}