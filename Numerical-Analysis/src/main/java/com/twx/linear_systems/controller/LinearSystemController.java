// 文件路径: src/main/java/com/twx/linear_systems/controller/LinearSystemController.java
package com.twx.linear_systems.controller;

import com.twx.linear_systems.model.*;
import com.twx.linear_systems.model.Direct_impl.*;

import com.twx.linear_systems.model.Iterative_impl.GaussSeidelSolver;
import com.twx.linear_systems.model.Iterative_impl.JacobiSolver;
import com.twx.linear_systems.model.Iterative_impl.SuccessiveOverRelaxationSolver;
import com.twx.linear_systems.view.ConvergencePlot;
import com.twx.linear_systems.view.MatrixView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LinearSystemController {

    // --- FXML Controls ---
    @FXML private Spinner<Integer> sizeSpinner;
    @FXML private ComboBox<String> methodTypeComboBox;
    @FXML private ComboBox<String> specificMethodComboBox;
    @FXML private Button resetButton, nextStepButton;
    @FXML private GridPane matrixInputGrid;
    @FXML private StackPane visualisationPane;
    @FXML private TextArea logArea;

    // 新增: 为SOR的omega参数添加UI容器
    @FXML private HBox sorControlsContainer;
    private TextField omegaField;


    // --- Solver Management ---
    private final List<DirectSolver> directSolvers = List.of(
            new CroutSolver(),
            new CompletePivotingGaussianSolver(),
            new SimpleGaussianEliminationSolver(),
            new GaussianEliminationSolver()
    );
    private final SuccessiveOverRelaxationSolver sorSolver = new SuccessiveOverRelaxationSolver(); // 单独实例化以便引用
    private final List<IterativeSolver> iterativeSolvers = List.of(
            sorSolver, // 将实例放入列表
            new GaussSeidelSolver(),
            new JacobiSolver()
    );

    private enum SolverType { DIRECT, ITERATIVE }
    private SolverType currentSolverType;

    // --- State Management ---
    private Iterator<MatrixState> directHistoryIterator;
    private Iterator<VectorIterationState> iterativeIterator;
    private RealVector finalSolution;

    // --- Views ---
    private final MatrixView matrixView = new MatrixView();
    private final ConvergencePlot convergencePlot = new ConvergencePlot();

    @FXML
    public void initialize() {
        // --- Spinner Setup ---
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 9, 3);
        sizeSpinner.setValueFactory(valueFactory);
        sizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> createMatrixInputGrid(newVal));

        // --- Omega (SOR) Controls Setup ---
        setupSorControls();

        // --- ComboBox Setup ---
        methodTypeComboBox.setItems(FXCollections.observableArrayList("直接法", "迭代法"));
        methodTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateSpecificMethodComboBox(newVal);
            }
        });

        // --- Button Actions ---
        resetButton.setOnAction(e -> initializeSolver());
        nextStepButton.setOnAction(e -> performNextStep());

        // --- Initial UI State ---
        createMatrixInputGrid(sizeSpinner.getValue());
        methodTypeComboBox.getSelectionModel().select(1);
    }

    /**
     * 创建并配置SOR方法的omega输入控件.
     */
    private void setupSorControls() {
        Label omegaLabel = new Label("Omega (ω):");
        omegaField = new TextField("1.2"); // 设置一个常用的默认值
        omegaField.setPromptText("0 < ω < 2");
        omegaField.setPrefWidth(80);
        sorControlsContainer.getChildren().addAll(omegaLabel, omegaField);
    }

    private void updateSpecificMethodComboBox(String methodType) {
        // 监听具体方法选择的改变，以控制omega输入框的可见性
        specificMethodComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSor = newVal != null && newVal.equals(sorSolver.getName());
            sorControlsContainer.setVisible(isSor);
            sorControlsContainer.setManaged(isSor); // managed为false时，控件不参与布局
        });

        if ("直接法".equals(methodType)) {
            specificMethodComboBox.setItems(FXCollections.observableArrayList(
                    directSolvers.stream().map(LinearSystemSolver::getName).collect(Collectors.toList())
            ));
            visualisationPane.getChildren().setAll(matrixView);
            currentSolverType = SolverType.DIRECT;
        } else if ("迭代法".equals(methodType)) {
            specificMethodComboBox.setItems(FXCollections.observableArrayList(
                    iterativeSolvers.stream().map(LinearSystemSolver::getName).collect(Collectors.toList())
            ));
            visualisationPane.getChildren().setAll(convergencePlot);
            currentSolverType = SolverType.ITERATIVE;
        }
        specificMethodComboBox.getSelectionModel().selectFirst();
    }

    private void initializeSolver() {
        try {
            // --- 1. 读取UI数据 ---
            int size = sizeSpinner.getValue();
            // ... (读取矩阵和向量的代码保持不变) ...
            double[][] aData = new double[size][size];
            double[] bData = new double[size];
            for (Node node : matrixInputGrid.getChildren()) {
                Integer col = GridPane.getColumnIndex(node);
                Integer row = GridPane.getRowIndex(node);
                if (col == null || row == null || !(node instanceof TextField tf)) continue;
                if (col < size) {
                    aData[row][col] = Double.parseDouble(tf.getText());
                } else if (col == size + 1) {
                    bData[row] = Double.parseDouble(tf.getText());
                }
            }
            RealMatrix a = new Array2DRowRealMatrix(aData);
            RealVector b = new ArrayRealVector(bData);


            clearAll();
            log("初始化求解器...");

            // --- 2. 根据选择调用求解器 ---
            String selectedMethodName = specificMethodComboBox.getSelectionModel().getSelectedItem();

            if (currentSolverType == SolverType.DIRECT) {
                DirectSolver solver = directSolvers.stream()
                        .filter(s -> s.getName().equals(selectedMethodName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("未找到指定的直接法求解器: " + selectedMethodName));
                DirectSolution solution = solver.solve(a, b);
                directHistoryIterator = solution.history().iterator();
                finalSolution = solution.solution();
            } else { // ITERATIVE
                IterativeSolver solver = iterativeSolvers.stream()
                        .filter(s -> s.getName().equals(selectedMethodName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("未找到指定的迭代法求解器: " + selectedMethodName));
                RealVector x0 = new ArrayRealVector(size, 0.0);

                // --- 核心改动: 检查是否为SOR方法并处理omega ---
                if (solver instanceof SuccessiveOverRelaxationSolver sor) {
                    double omega = Double.parseDouble(omegaField.getText());
                    if (omega <= 0 || omega >= 2) {
                        throw new IllegalArgumentException("Omega (ω) 值必须在 (0, 2) 范围内。");
                    }
                    log("使用 SOR 方法, ω = " + omega);
                    iterativeIterator = sor.createIterator(a, b, x0, omega, 1e-6, 100);
                } else {
                    // 对于其他迭代法，调用标准接口
                    iterativeIterator = solver.createIterator(a, b, x0, 1e-6, 100);
                }
            }

            performNextStep();
            nextStepButton.setDisable(false);

        } catch (NumberFormatException e) {
            log("错误: 输入无效，请确保所有输入均为数字。");
            nextStepButton.setDisable(true);
        } catch (Exception e) {
            log("错误: " + e.getMessage());
            e.printStackTrace();
            nextStepButton.setDisable(true);
        }
    }

    // --- performNextStep, displayFinalSolution, clearAll, 等其他方法保持不变 ---
    // ... (此处省略您原有的其他方法，无需改动)

    /**
     * "下一步" 按钮的事件处理器. 统一处理两种类型的求解过程.
     */
    private void performNextStep() {
        if (currentSolverType == SolverType.DIRECT) {
            if (directHistoryIterator != null && directHistoryIterator.hasNext()) {
                MatrixState state = directHistoryIterator.next();
                matrixView.updateMatrix(state);
                log(state.description());
                if (!directHistoryIterator.hasNext()) {
                    nextStepButton.setDisable(true);
                    displayFinalSolution();
                }
            }
        } else { // ITERATIVE
            if (iterativeIterator != null && iterativeIterator.hasNext()) {
                VectorIterationState state = iterativeIterator.next();
                convergencePlot.addState(state);
                log(String.format("k=%d, residual=%.6e, x=%s", state.k(), state.residualNorm(), formatVector(state.x_k())));
                if (!iterativeIterator.hasNext()) {
                    nextStepButton.setDisable(true);
                    log("迭代结束。");
                }
            }
        }
    }

    /**
     * 在日志区显示直接法的最终解.
     */
    private void displayFinalSolution() {
        if (finalSolution != null) {
            String separator = "\n" + "-".repeat(40) + "\n";
            log(separator + "求解完成！最终解 x = " + formatVector(finalSolution));
        } else {
            log("\n" + "-".repeat(40) + "\n求解失败，矩阵可能奇异或无唯一解。");
        }
    }

    /**
     * 清理所有状态和UI组件.
     */
    private void clearAll() {
        logArea.clear();
        directHistoryIterator = null;
        iterativeIterator = null;
        finalSolution = null;
        matrixView.getChildren().clear();
        convergencePlot.clear();
        nextStepButton.setDisable(true);
    }
    private void createMatrixInputGrid(int size) {
        matrixInputGrid.getChildren().clear();
        matrixInputGrid.setAlignment(Pos.CENTER);
        double[][] sampleA = {{4, 1, -1, 1}, {1, 4, 1, -1}, {-1, 1, 5, 1}, {1, -1, 1, 3}};
        double[] sampleB = {6, 5, 1, 1};

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                TextField tf = new TextField();
                tf.setPrefWidth(50);
                if (i < sampleA.length && j < sampleA[i].length) {
                    tf.setText(String.valueOf(sampleA[i][j]));
                }
                matrixInputGrid.add(tf, j, i);
            }
            Label separator = new Label("|");
            separator.getStyleClass().add("matrix-separator-label");
            matrixInputGrid.add(separator, size, i);

            TextField tf = new TextField();
            tf.setPrefWidth(50);
            if (i < sampleB.length) {
                tf.setText(String.valueOf(sampleB[i]));
            }
            matrixInputGrid.add(tf, size + 1, i);
        }
    }
    private void log(String message) {
        logArea.appendText(message + "\n");
    }
    private String formatVector(RealVector vector) {
        return IntStream.range(0, vector.getDimension())
                .mapToDouble(vector::getEntry)
                .mapToObj(d -> String.format("%.4f", d))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}