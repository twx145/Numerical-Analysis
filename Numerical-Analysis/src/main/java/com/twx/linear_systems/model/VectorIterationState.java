// 文件路径: src/com/twx/linear_systems/model/VectorIterationState.java
package com.twx.linear_systems.model;

import org.apache.commons.math3.linear.RealVector;

public record VectorIterationState(int k, RealVector x_k, double residualNorm) {}