package com.twx.iterative_methods.model.impl;

import java.util.function.Function;

public class Equation {

    private final ManualExpression f, g;
    private static final double H = 1e-7; // 微分小步长

    // 构造函数使用我们自己的 ManualExpression
    public Equation(String fStr, String gStr) {
        this.f = new ManualExpression(fStr);
        if (gStr != null && !gStr.isBlank()) {
            this.g = new ManualExpression(gStr);
        } else {
            this.g = new ManualExpression(null); // 传入 null 来创建一个会返回 NaN 的表达式
        }
    }

    public Function<Double, Double> getF() {
        return f::evaluate;
    }

    public Function<Double, Double> getG() {
        return g::evaluate;
    }

    // 自动计算导数 (这部分逻辑不变，因为它是数值方法，不依赖于表达式库)
    public Function<Double, Double> getDf() {
        // 使用中心差分公式: (f(x+h) - f(x-h)) / (2h)
        return (x) -> {
            try {
                // 直接调用我们自己的 evaluate 方法
                double f_x_plus_h = f.evaluate(x + H);
                double f_x_minus_h = f.evaluate(x - H);
                return (f_x_plus_h - f_x_minus_h) / (2 * H);
            } catch (Exception e) {
                // 我们的求值器在遇到问题时可能会抛出异常或返回NaN
                return Double.NaN;
            }
        };
    }
}