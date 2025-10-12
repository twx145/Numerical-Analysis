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
    @FXML private HBox intervalContainer;
    @FXML private Spinner<Integer> intervalSpinner;


    // --- 状态管理变量 ---
    private Equation currentEquation;
    private MethodIterator currentIterator;
    private final List<IterationState> iterationHistory = new ArrayList<>();
    // --- 修改：添加了 AitkenMethod ---
    private final IterativeMethod[] methods = {
            new SimpleIterationMethod(), new NewtonMethod(), new AitkenMethod(),
            new SimplifiedNewtonMethod(), new ModifiedSecantMethod(), new DampedNewtonMethod(),
            new SinglePointSecantMethod(), new DoublePointSecantMethod()
    };
    // --- 修改：添加了新方法的名称变量 ---
    private final String simpleIterationName = new SimpleIterationMethod().getName();
    private final String secantMethodName = new DoublePointSecantMethod().getName();
    private final String modifiedSecantMethodName = new ModifiedSecantMethod().getName();
    private final String aitkenMethodName = new AitkenMethod().getName(); // 新增
    private final String singlePointSecantMethodName = new SinglePointSecantMethod().getName();

    @FXML
    public void initialize() {
        methodComboBox.setItems(FXCollections.observableArrayList(
                Stream.of(methods).map(IterativeMethod::getName).toList()
        ));
        methodComboBox.getSelectionModel().select(1); // Default to Newton's method

        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 3, 1);
        intervalSpinner.setValueFactory(valueFactory);

        methodComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateUiForSelectedMethod());
        updateUiForSelectedMethod();

        resetButton.setOnAction(e -> initializeIteration());
        nextStepButton.setOnAction(e -> performNextStep());
        clearButton.setOnAction(e -> clearAll());
    }

    // 在 MainController.java 中，替换整个方法
    private void initializeIteration() {
        try {
            clearAll();
            iterationHistory.clear();

            String selectedMethodName = methodComboBox.getSelectionModel().getSelectedItem();
            String gStr = (selectedMethodName.equals(simpleIterationName) || selectedMethodName.equals(aitkenMethodName))
                    ? gField.getText() : "";

            currentEquation = new Equation(fField.getText(), gStr);
            double x0 = Double.parseDouble(initialValueField.getText());

            IterativeMethod selectedMethod = methods[methodComboBox.getSelectionModel().getSelectedIndex()];

            if (selectedMethod instanceof ModifiedSecantMethod) {
                int interval = intervalSpinner.getValue();
                ((ModifiedSecantMethod) selectedMethod).setUpdateInterval(interval);
            }

            twoDimPlot.setPlotData(currentEquation, selectedMethod, iterationHistory);
            oneDimPlot.setPlotData(iterationHistory, getColorForMethod(selectedMethod));

            // --- 核心修改在这里 ---
            // 判断方法是否为“割线法”或“单点弦截法”
            if (selectedMethod.getName().equals(secantMethodName) || selectedMethod.getName().equals(singlePointSecantMethodName)) {
                // 如果是，就读取第二个输入框的值，并调用双参数的createIterator
                double x1 = Double.parseDouble(secondInitialValueField.getText());
                currentIterator = selectedMethod.createIterator(currentEquation, x0, x1);
            } else {
                // 否则，调用单参数的createIterator
                currentIterator = selectedMethod.createIterator(currentEquation, x0);
            }

            if (currentIterator.hasNext()) {
                IterationState initialState = currentIterator.next();
                iterationHistory.add(initialState);
                logInitialState(initialState);
            } else {
                showError("Initialization Error", "Could not create iterator. Check function and initial value.");
                return;
            }

            drawFunctionWithInitialBounds(x0);
            nextStepButton.setDisable(false);

        } catch (Exception e) {
            // 这里就是捕捉到并显示您看到的错误的“功臣”
            showError("Input Invalid", "Please check function expressions or parameters.\nError: " + e.getMessage());
            nextStepButton.setDisable(true);
        }
    }

    private void performNextStep() {
        if (currentIterator == null || !currentIterator.hasNext()) {
            nextStepButton.setDisable(true);
            return;
        }
        IterationState newState = currentIterator.next();
        iterationHistory.add(newState);
        logIterationStep(newState);
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

    private void clearAll() {
        currentIterator = null;
        iterationHistory.clear();
        if (twoDimPlot != null) twoDimPlot.setPlotData(null, null, null);
        if (oneDimPlot != null) oneDimPlot.setPlotData(Collections.emptyList(), Color.BLACK);
        if (logArea != null) logArea.clear();
        if (nextStepButton != null) nextStepButton.setDisable(true);
    }

    private void logInitialState(IterationState state) {
        logArea.setText(String.format("%-4s | %-18s | %-18s | %-18s | %-18s\n", "k", "x_k", "f(x_k)", "|x_k - x_{k-1}|", "Ratio"));
        logArea.appendText("-".repeat(85) + "\n");
        logArea.appendText(String.format("%-4d | %-18.12f | %-18.12f | %-18s | %-18s\n", state.k(), state.x_k(), state.fx_k(), "N/A", "N/A"));
    }

    private void logIterationStep(IterationState state) {
        logArea.appendText(String.format("%-4d | %-18.12f | %-18.12f | %-18.12e | %-18.12f\n", state.k(), state.x_k(), state.fx_k(), state.error_abs(), state.error_ratio()));
    }

    private void drawFunctionWithInitialBounds(double x0) {
        double range = 5.0;
        double y_at_x0;
        try { y_at_x0 = currentEquation.getF().apply(x0); } catch (Exception e) { y_at_x0 = 0.0; }
        twoDimPlot.animateToNewRange(x0 - range / 2, x0 + range / 2, y_at_x0 - range / 2, y_at_x0 + range / 2, currentEquation.getF(), currentEquation.getG());
    }

    private double[] calculateDynamicBounds2D(IterationState state, IterativeMethod method) {
        double x1 = state.x_k_minus_1(), x2 = state.x_k();
        double minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        double paddingX = Math.max((maxX - minX) * 0.5, 0.01);
        double finalMinX = minX - paddingX, finalMaxX = maxX + paddingX;
        double minY, maxY;
        // --- 修改：艾特肯法也需要聚焦于 y=g(x) ---
        if (method instanceof SimpleIterationMethod || method instanceof AitkenMethod) {
            minY = Double.POSITIVE_INFINITY; maxY = Double.NEGATIVE_INFINITY;
            for (int i = 0; i <= 100; i++) {
                double x = finalMinX + i * (finalMaxX - finalMinX) / 100;
                try {
                    double valG = currentEquation.getG().apply(x);
                    if (Double.isFinite(valG)) { minY = Math.min(minY, valG); maxY = Math.max(maxY, valG); }
                } catch (Exception ignored) {}
            }
            minY = Math.min(minY, finalMinX); maxY = Math.max(maxY, finalMaxX);
        } else {
            minY = Double.POSITIVE_INFINITY; maxY = Double.NEGATIVE_INFINITY;
            for (int i = 0; i <= 100; i++) {
                double x = finalMinX + i * (finalMaxX - finalMinX) / 100;
                try {
                    double valF = currentEquation.getF().apply(x);
                    if (Double.isFinite(valF)) { minY = Math.min(minY, valF); maxY = Math.max(maxY, valF); }
                } catch (Exception ignored) {}
            }
            minY = Math.min(minY, 0); maxY = Math.max(maxY, 0);
        }
        if (!Double.isFinite(minY) || !Double.isFinite(maxY)) { minY = -5; maxY = 5; }
        double paddingY = Math.max((maxY - minY) * 0.2, 0.01);
        return new double[]{finalMinX, finalMaxX, minY - paddingY, maxY + paddingY};
    }

    private double[] calculateDynamicBounds1D(IterationState state) {
        double x1 = state.x_k_minus_1(), x2 = state.x_k();
        double minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        double paddingX = Math.max((maxX - minX) * 1.5, 0.01);
        return new double[]{minX - paddingX, maxX + paddingX};
    }

    private void updateUiForSelectedMethod() {
        String selected = methodComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // --- 修改：艾特肯法也需要显示 g(x) 输入框 ---
        boolean isSimpleOrAitken = selected.equals(simpleIterationName) || selected.equals(aitkenMethodName);
        gLabel.setVisible(isSimpleOrAitken); gField.setVisible(isSimpleOrAitken);
        gLabel.setManaged(isSimpleOrAitken); gField.setManaged(isSimpleOrAitken);

        boolean isSecant = selected.equals(secantMethodName) || selected.equals(singlePointSecantMethodName);
        x1Container.setVisible(isSecant);
        x1Container.setManaged(isSecant);

        boolean isModifiedSecant = selected.equals(modifiedSecantMethodName);
        intervalContainer.setVisible(isModifiedSecant);
        intervalContainer.setManaged(isModifiedSecant);
    }

    private Color getColorForMethod(IterativeMethod method) {
        if (method instanceof NewtonMethod) return Color.BLUE;
        // --- 新增：为艾特肯法和斯蒂芬森法分配不同颜色 ---
        if (method instanceof AitkenMethod) return Color.DEEPPINK;
        if (method instanceof SimplifiedNewtonMethod) return Color.rgb(0, 100, 100);
        if (method instanceof ModifiedSecantMethod) return Color.rgb(255, 140, 0);
        if (method instanceof DampedNewtonMethod) return Color.rgb(75, 0, 130);
        if (method instanceof SinglePointSecantMethod) return Color.ORANGE;
        if (method instanceof DoublePointSecantMethod) return Color.PURPLE;
        return Color.RED; // Default
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}