// 文件路径: src/main/java/com/twx/linear_systems/model/GaussSeidelSolver.java
package com.twx.linear_systems.model.Iterative_impl;

import com.twx.linear_systems.model.IterativeSolver;
import com.twx.linear_systems.model.VectorIterationState;
import org.apache.commons.math3.linear.*;
import java.util.Iterator;

public class GaussSeidelSolver implements IterativeSolver {

    @Override
    public String getName() {
        return "高斯-赛德尔迭代法";
    }

    @Override
    public Iterator<VectorIterationState> createIterator(RealMatrix a, RealVector b, RealVector x0, double tol, int maxIter) {
        return new Iterator<>() {
            private int k = 0;
            private RealVector x = x0.copy();
            private double residualNorm = a.operate(x).subtract(b).getNorm();

            @Override
            public boolean hasNext() {
                // 首次调用 next() 总是返回初始状态
                if (k == 0) return true;
                // 检查是否达到最大迭代次数或残差是否小于容忍度
                return k <= maxIter && residualNorm > tol;
            }

            @Override
            public VectorIterationState next() {
                if (k == 0) {
                    k++;
                    return new VectorIterationState(0, x0.copy(), residualNorm);
                }

                RealVector x_new = x.copy();
                for (int i = 0; i < a.getRowDimension(); i++) {
                    double sigma1 = 0;
                    for (int j = 0; j < i; j++) {
                        sigma1 += a.getEntry(i, j) * x_new.getEntry(j);
                    }

                    double sigma2 = 0;
                    for (int j = i + 1; j < a.getColumnDimension(); j++) {
                        sigma2 += a.getEntry(i, j) * x.getEntry(j);
                    }

                    x_new.setEntry(i, (b.getEntry(i) - sigma1 - sigma2) / a.getEntry(i, i));
                }

                x = x_new;
                residualNorm = a.operate(x).subtract(b).getNorm();

                return new VectorIterationState(k++, x.copy(), residualNorm);
            }
        };
    }
}