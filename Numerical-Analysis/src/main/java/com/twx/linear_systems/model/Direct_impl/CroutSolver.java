// 文件路径: src/main/java/com/twx/linear_systems/model/Direct_impl/NormalizedGaussianEliminationSolver.java
package com.twx.linear_systems.model.Direct_impl;

import com.twx.linear_systems.model.DirectSolution;
import com.twx.linear_systems.model.DirectSolver;
import com.twx.linear_systems.model.MatrixState;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;

/**
 * 带主元归一化的高斯消元法
 * 该方法在前向消元的每一步将主元变为1，然后消除该主元下方的所有元素，
 * 最终形成一个主对角线为1的上三角矩阵，再通过回代求解。
 */
public class CroutSolver implements DirectSolver {

    private static final double EPSILON = 1e-10;

    @Override
    public String getName() {
        return "克劳特消元法";
    }

    @Override
    public DirectSolution solve(RealMatrix a, RealVector b) {
        List<MatrixState> history = new ArrayList<>();
        int n = b.getDimension();
        RealMatrix aug = new Array2DRowRealMatrix(n, n + 1);
        aug.setSubMatrix(a.getData(), 0, 0);
        aug.setColumnVector(n, b);

        history.add(new MatrixState("初始增广矩阵", aug.copy(), null));

        // 1. 前向消元 (带主元归一化)
        for (int i = 0; i < n; i++) {
            double pivot = aug.getEntry(i, i);
            if (Math.abs(pivot) < EPSILON) {
                history.add(new MatrixState("错误: 主元 A(" + (i + 1) + "," + (i + 1) + ") 为零或过小，无法继续", aug.copy(), new int[]{i}));
                return new DirectSolution(history, null);
            }

            // a. 归一化当前行，使主元变为1
            if (Math.abs(pivot - 1.0) > EPSILON) {
                aug.setRowVector(i, aug.getRowVector(i).mapDivide(pivot));
                String desc = String.format("归一化: R%d = R%d / %.3f", i + 1, i + 1, pivot);
                history.add(new MatrixState(desc, aug.copy(), new int[]{i}));
            }

            // b. 对当前主元下方的所有行进行消元
            for (int j = i + 1; j < n; j++) {
                double factor = aug.getEntry(j, i);
                if (Math.abs(factor) < EPSILON) continue;

                // 执行行变换: Rj = Rj - factor * Ri
                RealVector rowI = aug.getRowVector(i).mapMultiply(factor);
                RealVector rowJ = aug.getRowVector(j).subtract(rowI);
                aug.setRowVector(j, rowJ);

                String desc = String.format("行变换: R%d = R%d - (%.3f) * R%d", j + 1, j + 1, factor, i + 1);
                history.add(new MatrixState(desc, aug.copy(), new int[]{i, j}));
            }
        }
        history.add(new MatrixState("前向消元完成，形成主对角线为1的上三角矩阵", aug.copy(), null));

        // 2. 回代求解 (与原始逻辑一致)
        // 由于主元都已归一化为1，回代过程中的除法步骤将变得简单
        double[] x = new double[n];
        RealMatrix displayMatrix = aug.copy();
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            StringBuilder sumDesc = new StringBuilder();
            for (int j = i + 1; j < n; j++) {
                double term = aug.getEntry(i, j) * x[j];
                sum += term;
                if (sumDesc.length() > 0) sumDesc.append(" + ");
                sumDesc.append(String.format("%.2f*x%d(%.2f)", aug.getEntry(i, j), j + 1, x[j]));
            }

            double originalB = aug.getEntry(i, n);
            double newB = originalB - sum;
            // 此处 A(i,i) 恒为 1
            double divisor = aug.getEntry(i, i);
            String substitutionDesc = (sumDesc.length() > 0)
                    ? String.format("回代 R%d: b' = %.2f - (%s) = %.2f", i + 1, originalB, sumDesc, newB)
                    : String.format("回代 R%d: 方程已简化", i + 1);

            for (int j = i + 1; j < n; j++) displayMatrix.setEntry(i, j, 0.0);
            displayMatrix.setEntry(i, n, newB);
            history.add(new MatrixState(substitutionDesc, displayMatrix.copy(), new int[]{i}));

            // 因为 divisor 总是 1, 所以 x[i] = newB
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