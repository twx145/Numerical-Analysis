// 文件路径: src/main/java/com/twx/linear_systems/model/DirectSolver.java
package com.twx.linear_systems.model;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * 直接法求解器的接口，一次性返回所有步骤和结果.
 */
public interface DirectSolver extends LinearSystemSolver {
    /**
     * 求解线性方程组.
     * @param a 系数矩阵 A
     * @param b 常数向量 b
     * @return 包含所有求解步骤和最终解的封装对象
     */
    DirectSolution solve(RealMatrix a, RealVector b);
}