package com.twx.iterative_methods.model;

/**
 * 一个记录类 (Record), 用于封装单次迭代的所有状态信息。
 * 这使得在模型、控制器和视图之间传递数据变得干净、类型安全。
 *
 * @param k             当前迭代次数 (从0开始)
 * @param x_k           当前迭代点的值 x_k
 * @param x_k_minus_1   上一个迭代点的值 x_{k-1} (对于k=0时可能为NaN)
 * @param fx_k          当前点对应的函数值 f(x_k)
 * @param error_abs     本次迭代的绝对误差 |x_k - x_{k-1}|
 * @param error_ratio   收敛速度的估计值 |x_k - x_{k-1}| / |x_{k-1} - x_{k-2}|
 */
public record IterationState(
        int k,
        double x_k,
        double x_k_minus_1,
        double fx_k,
        double error_abs,
        double error_ratio
) {
    // 为初始状态 (k=0) 提供一个方便的工厂方法
    public static IterationState initial(double x0, double fx0) {
        return new IterationState(0, x0, Double.NaN, fx0, Double.NaN, Double.NaN);
    }
}