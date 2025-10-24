// 文件路径: src/main/java/com/twx/linear_systems/model/IterativeSolver.java
package com.twx.linear_systems.model;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import java.util.Iterator;

/**
 * 迭代法求解器的接口，返回一个可以逐一获取状态的迭代器.
 */
public interface IterativeSolver extends LinearSystemSolver {
    /**
     * 创建一个迭代器来逐步求解线性方程组.
     * @param a 系数矩阵 A
     * @param b 常数向量 b
     * @param x0 初始解向量
     * @param tol 容差
     * @param maxIter 最大迭代次数
     * @return 包含迭代状态的迭代器
     */
    Iterator<VectorIterationState> createIterator(RealMatrix a, RealVector b, RealVector x0, double tol, int maxIter);
}