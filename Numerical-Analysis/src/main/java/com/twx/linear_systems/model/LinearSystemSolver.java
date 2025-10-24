// 文件路径: src/main/java/com/twx/linear_systems/model/LinearSystemSolver.java
package com.twx.linear_systems.model;

/**
 * 所有线性方程组求解器的通用基础接口.
 */
public interface LinearSystemSolver {
    /**
     * 获取求解器的名称，用于在UI中显示.
     * @return 求解器名称
     */
    String getName();
}