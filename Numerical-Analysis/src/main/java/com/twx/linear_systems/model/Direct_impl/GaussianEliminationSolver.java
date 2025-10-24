// 文件路径: src/main/java/com/twx/linear_systems/model/GaussianEliminationSolver.java
package com.twx.linear_systems.model.Direct_impl;

import com.twx.linear_systems.model.DirectSolution;
import com.twx.linear_systems.model.DirectSolver;
import com.twx.linear_systems.model.MatrixState;
import org.apache.commons.math3.linear.*;
import java.util.ArrayList;
import java.util.List;

public class GaussianEliminationSolver implements DirectSolver {

    private static final double EPSILON = 1e-10;

    @Override
    public String getName() {
        return "高斯列主元消元法";
    }

    @Override
    public DirectSolution solve(RealMatrix a, RealVector b) {
        // --- 这里的代码与你之前优化后的 GaussianElimination.java 里的 solve 方法完全相同 ---
        List<MatrixState> history = new ArrayList<>();
        int n = b.getDimension();
        RealMatrix aug = new Array2DRowRealMatrix(n, n + 1);
        aug.setSubMatrix(a.getData(), 0, 0);
        aug.setColumnVector(n, b);

        history.add(new MatrixState("初始增广矩阵", aug.copy(), null));

        // 1. 前向消元
        for (int i = 0; i < n; i++) {
            int max = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(aug.getEntry(j, i)) > Math.abs(aug.getEntry(max, i))) {
                    max = j;
                }
            }
            if (i != max) {
                double[] temp = aug.getRow(i);
                aug.setRow(i, aug.getRow(max));
                aug.setRow(max, temp);
                history.add(new MatrixState("行交换: R" + (i + 1) + " <-> R" + (max + 1), aug.copy(), new int[]{i, max}));
            }
            if (Math.abs(aug.getEntry(i, i)) < EPSILON) {
                history.add(new MatrixState("错误: 主元过小, 矩阵奇异或接近奇异", aug.copy(), new int[]{i}));
                return new DirectSolution(history, null);
            }
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
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += aug.getEntry(i, j) * x[j];
            }
            x[i] = (aug.getEntry(i, n) - sum) / aug.getEntry(i, i);
            history.add(new MatrixState(String.format("回代计算: x%d = %.4f", i + 1, x[i]), aug.copy(), new int[]{i}));
        }

        RealVector solutionVector = new ArrayRealVector(x);
        history.add(new MatrixState("回代完成，得到最终解", aug.copy(), null));

        return new DirectSolution(history, solutionVector);
    }
}