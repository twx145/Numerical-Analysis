// 文件路径: src/main/java/com/twx/linear_systems/model/Iterative_impl/SuccessiveOverRelaxationSolver.java
package com.twx.linear_systems.model.Iterative_impl;

import com.twx.linear_systems.model.IterativeSolver;
import com.twx.linear_systems.model.VectorIterationState;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Iterator;

public class SuccessiveOverRelaxationSolver implements IterativeSolver {

    @Override
    public String getName() {
        return "逐次超松弛迭代法";
    }

    /**
     * 满足 IterativeSolver 接口的方法。
     * 当不指定 omega 时，默认 omega=1.0，此时算法等价于 Gauss-Seidel 法。
     */
    @Override
    public Iterator<VectorIterationState> createIterator(RealMatrix a, RealVector b, RealVector x0, double tol, int maxIter) {
        // 默认使用 omega = 1.0
        return this.createIterator(a, b, x0, 1.0, tol, maxIter);
    }

    /**
     * SOR 方法的核心实现，允许指定 omega 值。
     * @param omega 松弛因子 (推荐 0 < omega < 2)
     * @return 包含迭代状态的迭代器
     */
    public Iterator<VectorIterationState> createIterator(RealMatrix a, RealVector b, RealVector x0, double omega, double tol, int maxIter) {
        return new Iterator<>() {
            private int k = 0;
            private RealVector x = x0.copy();
            private double residualNorm = a.operate(x).subtract(b).getNorm();

            @Override
            public boolean hasNext() {
                if (k == 0) return true;
                return k <= maxIter && residualNorm > tol;
            }

            @Override
            public VectorIterationState next() {
                if (k == 0) {
                    k++;
                    return new VectorIterationState(0, x0.copy(), residualNorm);
                }

                RealVector x_old = x.copy();

                for (int i = 0; i < a.getRowDimension(); i++) {
                    double sigma1 = 0;
                    for (int j = 0; j < i; j++) {
                        sigma1 += a.getEntry(i, j) * x.getEntry(j);
                    }

                    double sigma2 = 0;
                    for (int j = i + 1; j < a.getColumnDimension(); j++) {
                        sigma2 += a.getEntry(i, j) * x_old.getEntry(j);
                    }

                    double gs_component = (b.getEntry(i) - sigma1 - sigma2) / a.getEntry(i, i);

                    double new_xi = (1 - omega) * x_old.getEntry(i) + omega * gs_component;
                    x.setEntry(i, new_xi);
                }

                residualNorm = a.operate(x).subtract(b).getNorm();
                return new VectorIterationState(k++, x.copy(), residualNorm);
            }
        };
    }
}