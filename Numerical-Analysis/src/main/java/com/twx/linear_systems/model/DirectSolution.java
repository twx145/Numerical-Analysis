// 文件路径: src/main/java/com/twx/linear_systems/model/DirectSolution.java
package com.twx.linear_systems.model;

import org.apache.commons.math3.linear.RealVector;
import java.util.List;

/**
 * 封装直接法求解的结果，包括过程历史和最终解.
 */
public record DirectSolution(List<MatrixState> history, RealVector solution) {
}