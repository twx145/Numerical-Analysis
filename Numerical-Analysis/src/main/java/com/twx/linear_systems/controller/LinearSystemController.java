// 文件路径: src/main/java/com/twx/linear_systems/controller/LinearSystemController.java
package com.twx.linear_systems.controller;

import com.twx.linear_systems.model.*;
import com.twx.linear_systems.model.Direct_impl.GaussianEliminationSolver;
import com.twx.linear_systems.model.Iterative_impl.JacobiSolver;
import com.twx.linear_systems.view.ConvergencePlot;
import com.twx.linear_systems.view.MatrixView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
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

    // --- Solver Management (Decoupled Strategy) ---
    private final List<DirectSolver> directSolvers = List.of(
            // 未来可在这里添加 new LUSolver(), new CholeskySolver() 等
            new GaussianEliminationSolver()
    );
    private final List<IterativeSolver> iterativeSolvers = List.of(
            // 未来可在这里添加 new GaussSeidelSolver(), new SORSolver() 等
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
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 6, 3);
        sizeSpinner.setValueFactory(valueFactory);
        sizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> createMatrixInputGrid(newVal));

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
        methodTypeComboBox.getSelectionModel().selectFirst();
    }

    /**
     * 当解法类型改变时，更新具体方法的下拉框内容，并切换可视化面板.
     * @param methodType "直接法" 或 "迭代法"
     */
    private void updateSpecificMethodComboBox(String methodType) {
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

    /**
     * "开始/重置" 按钮的事件处理器. 读取输入，选择合适的求解器，并初始化求解过程.
     */
    private void initializeSolver() {
        try {
            // --- 1. 从UI读取矩阵和向量数据 ---
            int size = sizeSpinner.getValue();
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

            // --- 2. 根据选择，解耦地调用求解器 ---
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
                iterativeIterator = solver.createIterator(a, b, x0, 1e-6, 100);
            }

            performNextStep(); // 显示初始状态
            nextStepButton.setDisable(false);

        } catch (Exception e) {
            log("错误: 请检查输入数据或算法选择。\n" + e.getMessage());
            e.printStackTrace(); // 方便调试
            nextStepButton.setDisable(true);
        }
    }

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

    /**
     * 在UI上创建用于输入矩阵和向量的网格.
     * @param size 矩阵的大小 (n x n)
     */
    private void createMatrixInputGrid(int size) {
        matrixInputGrid.getChildren().clear();
        matrixInputGrid.setAlignment(Pos.CENTER);
        // 提供一组默认的示例数据，方便测试
        double[][] sampleA = {{4, 1, -1, 1}, {1, 4, 1, -1}, {-1, 1, 5, 1}, {1, -1, 1, 3}};
        double[] sampleB = {6, 5, 1, 1}; // 解为 [1, 1, 0, 0] (近似)

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

    /**
     * 在日志区域追加一条消息.
     * @param message 要显示的消息
     */
    private void log(String message) {
        logArea.appendText(message + "\n");
    }

    /**
     * 将一个 RealVector 格式化为易于阅读的字符串.
     * @param vector 要格式化的向量
     * @return 格式化后的字符串, 如 "[1.0000, 2.5000]"
     */
    private String formatVector(RealVector vector) {
        return IntStream.range(0, vector.getDimension())
                .mapToDouble(vector::getEntry)
                .mapToObj(d -> String.format("%.4f", d))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}