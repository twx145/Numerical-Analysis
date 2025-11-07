// 文件路径: src/main/java/com/twx/linear_systems/model/Direct_impl/SimpleGaussianEliminationSolver.java
package com.twx.linear_systems.model.Direct_impl;

import com.twx.linear_systems.model.DirectSolution;
import com.twx.linear_systems.model.DirectSolver;
import com.twx.linear_systems.model.MatrixState;
import org.apache.commons.math3.linear.*;
import java.util.ArrayList;
import java.util.List;

public class SimpleGaussianEliminationSolver implements DirectSolver {

    private static final double EPSILON = 1e-10;

    @Override
    public String getName() {
        return "高斯消元法";
    }

    @Override
    public DirectSolution solve(RealMatrix a, RealVector b) {
        List<MatrixState> history = new ArrayList<>();
        int n = b.getDimension();
        RealMatrix aug = new Array2DRowRealMatrix(n, n + 1);
        aug.setSubMatrix(a.getData(), 0, 0);
        aug.setColumnVector(n, b);

        history.add(new MatrixState("初始增广矩阵", aug.copy(), null));

        // 1. 前向消元 (无主元选择)
        for (int i = 0; i < n; i++) {
            if (Math.abs(aug.getEntry(i, i)) < EPSILON) {
                history.add(new MatrixState("错误: 主元 A(" + (i + 1) + "," + (i + 1) + ") 为零或过小，无法继续", aug.copy(), new int[]{i}));
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

        // 2. 回代求解 (与之前版本完全相同)
        double[] x = new double[n];
        RealMatrix displayMatrix = aug.copy();
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            StringBuilder sumDesc = new StringBuilder();
            for (int j = i + 1; j < n; j++) {
                double term = aug.getEntry(i, j) * x[j];
                sum += term;
                if (!sumDesc.isEmpty()) sumDesc.append(" + ");
                sumDesc.append(String.format("%.2f*x%d(%.2f)", aug.getEntry(i, j), j + 1, x[j]));
            }

            double originalB = aug.getEntry(i, n);
            double newB = originalB - sum;
            double divisor = aug.getEntry(i, i);
            String substitutionDesc = (!sumDesc.isEmpty())
                    ? String.format("回代 R%d: b' = %.2f - (%s) = %.2f", i + 1, originalB, sumDesc, newB)
                    : String.format("回代 R%d: 方程已简化", i + 1);

            for (int j = i + 1; j < n; j++) displayMatrix.setEntry(i, j, 0.0);
            displayMatrix.setEntry(i, n, newB);
            history.add(new MatrixState(substitutionDesc, displayMatrix.copy(), new int[]{i}));

            x[i] = newB / divisor;
            String solveDesc = String.format("计算 x%d = b' / A%d,%d = %.3f / %.3f = %.4f", i + 1, i + 1, i + 1, newB, divisor, x[i]);
            displayMatrix.setEntry(i, i, 1.0);
            displayMatrix.setEntry(i, n, x[i]);
            history.add(new MatrixState(solveDesc, displayMatrix.copy(), new int[]{i}));
        }

        RealVector solutionVector = new ArrayRealVector(x);
        history.add(new MatrixState("回代完成，得到最终解", displayMatrix, null));
        return new DirectSolution(history, solutionVector);
    }
}