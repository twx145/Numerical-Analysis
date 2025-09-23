package com.twx.iterative_methods.model.impl;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import java.util.function.Function;

public class Equation {

    private final Expression f, g;
    private static final double H = 1e-7; // 用于数值微分的小步长

    // 构造函数不再需要导数表达式
    public Equation(String fStr, String gStr) {
        this.f = new ExpressionBuilder(fStr).variable("x").build();
        // g(x) 可能为空，如果是，则创建一个始终返回NaN的表达式
        if (gStr != null && !gStr.isBlank()) {
            this.g = new ExpressionBuilder(gStr).variable("x").build();
        } else {
            this.g = new ExpressionBuilder("0/0").variable("x").build(); // 返回NaN
        }
    }

    public Function<Double, Double> getF() {
        return (x) -> f.setVariable("x", x).evaluate();
    }

    public Function<Double, Double> getG() {
        return (x) -> g.setVariable("x", x).evaluate();
    }

    // 自动计算导数
    public Function<Double, Double> getDf() {
        // 使用中心差分公式: (f(x+h) - f(x-h)) / (2h)
        return (x) -> {
            try {
                double f_x_plus_h = f.setVariable("x", x + H).evaluate();
                double f_x_minus_h = f.setVariable("x", x - H).evaluate();
                return (f_x_plus_h - f_x_minus_h) / (2 * H);
            } catch (Exception e) {
                return Double.NaN;
            }
        };
    }
}