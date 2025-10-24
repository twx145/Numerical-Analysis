// 文件路径: src/com/twx/linear_systems/model/MatrixState.java
package com.twx.linear_systems.model;

import org.apache.commons.math3.linear.RealMatrix;

public record MatrixState(String description, RealMatrix matrix, int[] highlightedRows) {}