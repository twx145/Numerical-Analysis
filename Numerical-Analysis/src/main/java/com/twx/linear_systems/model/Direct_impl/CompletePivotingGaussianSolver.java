// 文件路径: src/main/java/com/twx/linear_systems/model/Direct_impl/CompletePivotingGaussianSolver.java
package com.twx.linear_systems.model.Direct_impl;

import com.twx.linear_systems.model.DirectSolution;
import com.twx.linear_systems.model.DirectSolver;
import com.twx.linear_systems.model.MatrixState;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class CompletePivotingGaussianSolver implements DirectSolver {

    private static final double EPSILON = 1e-10;

    @Override
    public String getName() {
        return "高斯全主元消元法";
    }

    @Override
    public DirectSolution solve(RealMatrix a, RealVector b) {
        List<MatrixState> history = new ArrayList<>();
        int n = b.getDimension();
        RealMatrix aug = new Array2DRowRealMatrix(n, n + 1);
        aug.setSubMatrix(a.getData(), 0, 0);
        aug.setColumnVector(n, b);

        // 用于追踪列交换的数组, 初始为 [0, 1, 2, ..., n-1]
        int[] colIndices = IntStream.range(0, n).toArray();

        history.add(new MatrixState("初始增广矩阵, 列顺序: " + Arrays.toString(colIndices), aug.copy(), null));

        // 1. 前向消元 (全主元选择)
        for (int i = 0; i < n; i++) {
            int pivotRow = i;
            int pivotCol = i;
            double maxVal = Math.abs(aug.getEntry(i, i));

            for (int row = i; row < n; row++) {
                for (int col = i; col < n; col++) {
                    if (Math.abs(aug.getEntry(row, col)) > maxVal) {
                        maxVal = Math.abs(aug.getEntry(row, col));
                        pivotRow = row;
                        pivotCol = col;
                    }
                }
            }

            // --- 行交换 ---
            if (i != pivotRow) {
                double[] temp = aug.getRow(i);
                aug.setRow(i, aug.getRow(pivotRow));
                aug.setRow(pivotRow, temp);
                history.add(new MatrixState("行交换: R" + (i + 1) + " <-> R" + (pivotRow + 1), aug.copy(), new int[]{i, pivotRow}));
            }

            // --- 列交换 ---
            if (i != pivotCol) {
                RealVector temp = aug.getColumnVector(i);
                aug.setColumnVector(i, aug.getColumnVector(pivotCol));
                aug.setColumnVector(pivotCol, temp);

                int tempIndex = colIndices[i];
                colIndices[i] = colIndices[pivotCol];
                colIndices[pivotCol] = tempIndex;

                history.add(new MatrixState("列交换: C" + (i + 1) + " <-> C" + (pivotCol + 1) +
                        ", 新列顺序: " + Arrays.toString(colIndices), aug.copy(), null));
            }

            if (Math.abs(aug.getEntry(i, i)) < EPSILON) {
                history.add(new MatrixState("错误: 主元过小, 矩阵奇异或接近奇异", aug.copy(), new int[]{i}));
                return new DirectSolution(history, null);
            }

            // 消元
            for (int j = i + 1; j < n; j++) {
                double factor = aug.getEntry(j, i) / aug.getEntry(i, i);
                if (Math.abs(factor) < EPSILON) continue;
                RealVector rowI = aug.getRowVector(i).mapMultiply(factor);
                RealVector rowJ = aug.getRowVector(j).subtract(rowI);
                aug.setRowVector(j, rowJ);
                String desc = String.format("行变换: R%d = R%d - (%.3f) * R%d", j + 1, j + 1, factor, i + 1);
                history.add(new MatrixState(desc, aug.copy(), new int[]{i, j}));
            }
        }
        history.add(new MatrixState("前向消元完成，形成上三角矩阵", aug.copy(), null));

        // 2. 回代求解
        double[] x_permuted = new double[n];
        // ======================= 代码修改部分开始 =======================
        // 创建一个专门用于可视化的矩阵副本
        RealMatrix displayMatrix = aug.copy();

        for (int i = n - 1; i >= 0; i--) {
            // 计算过程仍然使用未被修改的上三角矩阵 `aug`，保证精度
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += aug.getEntry(i, j) * x_permuted[j];
            }
            double newB = aug.getEntry(i, n) - sum;
            double divisor = aug.getEntry(i, i);
            x_permuted[i] = newB / divisor;

            // --- 更新可视化矩阵 `displayMatrix` ---
            // 为了清晰地展示回代过程，我们将已求解的行进行变换
            // 将该行的主元变为1，其他系数变为0，右侧常数项变为解，模拟出已求解的样子
            displayMatrix.setEntry(i, i, 1.0); // 主元变为1
            for (int j = i + 1; j < n; j++) {
                displayMatrix.setEntry(i, j, 0.0); // 主元右边的系数变为0
            }
            displayMatrix.setEntry(i, n, x_permuted[i]); // 右侧常数项更新为解

            // 将更新后的可视化矩阵存入历史记录
            String desc = String.format("回代(乱序): x'%d = (%.3f - %.3f) / %.3f = %.4f",
                    i + 1, aug.getEntry(i, n), sum, divisor, x_permuted[i]);
            history.add(new MatrixState(desc, displayMatrix.copy(), new int[]{i}));
        }
        // ======================= 代码修改部分结束 =======================

        // 3. 重排序解向量
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[colIndices[i]] = x_permuted[i];
        }

        RealVector solutionVector = new ArrayRealVector(x);
        // 使用最终的可视化矩阵（此时应该近似为一个单位阵和解向量的组合）
        history.add(new MatrixState("回代完成，并根据列交换重排序，得到最终解", displayMatrix, null));

        return new DirectSolution(history, solutionVector);
    }
}