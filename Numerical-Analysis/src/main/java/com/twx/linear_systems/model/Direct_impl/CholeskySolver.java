// 文件路径: src/main/java/com/twx/linear_systems/model/Direct_impl/SquareRootMethodSolver.java
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
 * 平方根法 (针对对称正定矩阵的Cholesky分解变体)
 * 此方法通过一种类似高斯消元的过程，将对称正定矩阵 A 转化为上三角矩阵 U，
 * 使得 A = U^T * U。同时对向量 b 进行相应变换，然后通过回代求解。
 * 该方法仅适用于对称正定矩阵。
 */
public class CholeskySolver implements DirectSolver {

    private static final double EPSILON = 1e-10;

    @Override
    public String getName() {
        return "平方根法 (类高斯消元)";
    }

    @Override
    public DirectSolution solve(RealMatrix a, RealVector b) {
        List<MatrixState> history = new ArrayList<>();
        int n = b.getDimension();
        RealMatrix aug = new Array2DRowRealMatrix(n, n + 1);
        aug.setSubMatrix(a.getData(), 0, 0);
        aug.setColumnVector(n, b);

        history.add(new MatrixState("初始增广矩阵", aug.copy(), null));

        // --- 步骤 1: 将增广矩阵 [A|b] 变换为 [U|y] ---
        // 这个过程看起来像高斯消元，但计算方式不同
        for (int i = 0; i < n; i++) {
            // a. 计算并更新对角线元素 A(i,i)
            double sumSq = 0.0;
            for (int k = 0; k < i; k++) {
                sumSq += Math.pow(aug.getEntry(k, i), 2);
            }
            double aii = aug.getEntry(i, i) - sumSq;
            if (aii < EPSILON) {
                history.add(new MatrixState("错误: 矩阵非正定或计算不稳定", aug.copy(), new int[]{i}));
                return new DirectSolution(history, null);
            }
            double newAii = Math.sqrt(aii);
            String descAii = String.format("更新主元 U(%d,%d) = sqrt(A(%d,%d) - ΣU_ki²) = sqrt(%.2f - %.2f) = %.3f",
                    i + 1, i + 1, i + 1, i + 1, aug.getEntry(i, i), sumSq, newAii);
            aug.setEntry(i, i, newAii);
            history.add(new MatrixState(descAii, aug.copy(), new int[]{i}));


            // b. 更新主元所在行的其余元素 (A(i,j) 和 b_i)
            for (int j = i + 1; j < n + 1; j++) { // j < n+1 以包含 b 向量部分
                double sumProd = 0.0;
                for (int k = 0; k < i; k++) {
                    sumProd += aug.getEntry(k, i) * aug.getEntry(k, j);
                }
                double newValue = (aug.getEntry(i, j) - sumProd) / newAii;
                String descRow = String.format("更新 U/y(%d,%d) = (A/b(%d,%d) - ΣU_ki*U_kj) / U(%d,%d) = %.3f",
                        i + 1, j + 1, i + 1, j + 1, i + 1, i + 1, newValue);
                aug.setEntry(i, j, newValue);
                history.add(new MatrixState(descRow, aug.copy(), new int[]{i,j}));
            }
            // 将下三角部分清零，使其在视觉上是上三角矩阵
            for (int j = 0; j < i; j++) {
                aug.setEntry(i, j, 0.0);
            }
        }
        history.add(new MatrixState("变换完成，形成上三角矩阵 U 和新向量 y", aug.copy(), null));

        // --- 步骤 2: 回代求解 Ux = y (与高斯消元法完全一致) ---
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
            double divisor = aug.getEntry(i, i);
            String substitutionDesc = (sumDesc.length() > 0)
                    ? String.format("回代 R%d: b' = %.2f - (%s) = %.2f", i + 1, originalB, sumDesc, newB)
                    : String.format("回代 R%d: 方程已简化", i + 1);

            // 更新显示矩阵
            for (int j = i + 1; j < n; j++) displayMatrix.setEntry(i, j, 0.0);
            displayMatrix.setEntry(i, n, newB);
            history.add(new MatrixState(substitutionDesc, displayMatrix.copy(), new int[]{i}));

            x[i] = newB / divisor;
            String solveDesc = String.format("计算 x%d = b' / U%d,%d = %.3f / %.3f = %.4f", i + 1, i + 1, i + 1, newB, divisor, x[i]);
            displayMatrix.setEntry(i, i, 1.0);
            displayMatrix.setEntry(i, n, x[i]);
            history.add(new MatrixState(solveDesc, displayMatrix.copy(), new int[]{i}));
        }

        RealVector solutionVector = new ArrayRealVector(x);
        history.add(new MatrixState("回代完成，得到最终解", displayMatrix, null));
        return new DirectSolution(history, solutionVector);
    }
}