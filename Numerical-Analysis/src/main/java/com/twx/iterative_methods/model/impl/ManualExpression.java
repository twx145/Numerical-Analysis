package com.twx.iterative_methods.model.impl;

import java.util.*;

public class ManualExpression {

    // --- 新增和修改的成员变量 ---

    // 支持的运算符
    private static final Map<String, Integer> PRECEDENCE = new HashMap<>();
    private static final Map<String, Boolean> ASSOCIATIVITY = new HashMap<>(); // true for left, false for right

    // 支持的函数及其参数个数
    private static final Map<String, Integer> FUNCTIONS = new HashMap<>();

    // 支持的常数
    private static final Map<String, Double> CONSTANTS = new HashMap<>();

    // 用于区分一元负号的特殊内部符号
    private static final String UNARY_MINUS = "~";

    // 静态代码块，用于初始化所有支持的符号
    static {
        // 运算符优先级和结合性
        PRECEDENCE.put("+", 2); ASSOCIATIVITY.put("+", true);
        PRECEDENCE.put("-", 2); ASSOCIATIVITY.put("-", true);
        PRECEDENCE.put("*", 3); ASSOCIATIVITY.put("*", true);
        PRECEDENCE.put("/", 3); ASSOCIATIVITY.put("/", true);
        PRECEDENCE.put("^", 4); ASSOCIATIVITY.put("^", false); // 幂运算是右结合
        PRECEDENCE.put(UNARY_MINUS, 5); ASSOCIATIVITY.put(UNARY_MINUS, false); // 一元负号优先级最高

        // 函数列表和它们的参数数量
        FUNCTIONS.put("sin", 1);
        FUNCTIONS.put("cos", 1);
        FUNCTIONS.put("tan", 1);
        FUNCTIONS.put("abs", 1);
        FUNCTIONS.put("sqrt", 1);
        FUNCTIONS.put("log", 1);   // 自然对数
        FUNCTIONS.put("log10", 1); // 以10为底的对数
        FUNCTIONS.put("pow", 2);
        FUNCTIONS.put("max", 2);
        FUNCTIONS.put("min", 2);

        // 常数
        CONSTANTS.put("pi", Math.PI);
        CONSTANTS.put("e", Math.E);
    }

    private final List<String> rpnExpression;
    private final Set<String> variables;

    public ManualExpression(String expressionStr) {
        if (expressionStr == null || expressionStr.isBlank()) {
            this.rpnExpression = Collections.emptyList();
            this.variables = Collections.emptySet();
            return;
        }
        // 词法分析和语法分析（调度场算法）
        List<String> tokens = tokenize(expressionStr);
        this.rpnExpression = shuntingYard(tokens);

        // 识别表达式中的变量
        this.variables = new HashSet<>();
        for (String token : tokens) {
            if (isVariable(token)) {
                this.variables.add(token);
            }
        }
    }

    // 步骤 1: 词法分析 (Tokenizer)
    private List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();

        // 为了方便处理，将所有符号前后加上空格
        expression = expression.replace("(", " ( ");
        expression = expression.replace(")", " ) ");
        expression = expression.replace(",", " , ");
        for (String op : PRECEDENCE.keySet()) {
            if (!op.equals(UNARY_MINUS)) { // 不处理内部符号
                expression = expression.replace(op, " " + op + " ");
            }
        }

        // 使用空格进行分割
        String[] rawTokens = expression.trim().split("\\s+");
        Collections.addAll(tokens, rawTokens);

        return tokens;
    }


    // 步骤 2: 调度场算法 (Shunting-yard) - 增加了完整的错误检查
    private List<String> shuntingYard(List<String> tokens) {
        List<String> outputQueue = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();
        String lastToken = null;

        for (String token : tokens) {
            if (isNumber(token) || CONSTANTS.containsKey(token.toLowerCase())) {
                outputQueue.add(token);
            } else if (isVariable(token)) {
                outputQueue.add(token);
            } else if (FUNCTIONS.containsKey(token.toLowerCase())) {
                operatorStack.push(token);
            } else if (token.equals(",")) {
                // 逗号用于分隔函数参数
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    outputQueue.add(operatorStack.pop());
                }
                if (operatorStack.isEmpty()) {
                    throw new IllegalArgumentException("非法输入: 逗号位置不当或括号不匹配。");
                }
            } else if (token.equals("-") && (lastToken == null || PRECEDENCE.containsKey(lastToken) || lastToken.equals("("))) {
                // *** 关键: 处理一元负号 ***
                // 如果'-'是第一个符号，或在操作符/左括号之后，则视为一元负号
                operatorStack.push(UNARY_MINUS);
            } else if (PRECEDENCE.containsKey(token)) { // 普通二元运算符
                while (!operatorStack.isEmpty() && PRECEDENCE.containsKey(operatorStack.peek())) {
                    String op2 = operatorStack.peek();
                    if ((ASSOCIATIVITY.get(token) && PRECEDENCE.get(token) <= PRECEDENCE.get(op2)) ||
                            (!ASSOCIATIVITY.get(token) && PRECEDENCE.get(token) < PRECEDENCE.get(op2))) {
                        outputQueue.add(operatorStack.pop());
                    } else {
                        break;
                    }
                }
                operatorStack.push(token);
            } else if (token.equals("(")) {
                operatorStack.push(token);
            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    outputQueue.add(operatorStack.pop());
                }
                if (operatorStack.isEmpty()) {
                    throw new IllegalArgumentException("非法输入: 括号不匹配。");
                }
                operatorStack.pop(); // 弹出 '('
                if (!operatorStack.isEmpty() && FUNCTIONS.containsKey(operatorStack.peek().toLowerCase())) {
                    outputQueue.add(operatorStack.pop());
                }
            } else {
                throw new IllegalArgumentException("非法输入: 包含无法识别的符号 '" + token + "'");
            }
            lastToken = token;
        }

        while (!operatorStack.isEmpty()) {
            String op = operatorStack.pop();
            if (op.equals("(")) {
                throw new IllegalArgumentException("非法输入: 括号不匹配。");
            }
            outputQueue.add(op);
        }

        return outputQueue;
    }

    // 步骤 3: RPN 求值 - 支持多参数和一元运算
    public double evaluate(double xValue) {
        if (variables.size() > 1 || (variables.size() == 1 && !variables.contains("x"))) {
            throw new IllegalArgumentException("该表达式需要 'x' 之外的变量，无法仅用 xValue 求值。");
        }

        Stack<Double> valueStack = new Stack<>();

        for (String token : rpnExpression) {
            if (isNumber(token)) {
                valueStack.push(Double.parseDouble(token));
            } else if (CONSTANTS.containsKey(token.toLowerCase())) {
                valueStack.push(CONSTANTS.get(token.toLowerCase()));
            } else if (token.equalsIgnoreCase("x")) {
                valueStack.push(xValue);
            } else if (PRECEDENCE.containsKey(token)) {
                // 运算符求值
                if (token.equals(UNARY_MINUS)) {
                    if (valueStack.isEmpty()) throw new IllegalArgumentException("非法输入: 运算符缺少操作数。");
                    valueStack.push(-valueStack.pop());
                } else {
                    if (valueStack.size() < 2) throw new IllegalArgumentException("非法输入: 运算符缺少操作数。");
                    double b = valueStack.pop();
                    double a = valueStack.pop();
                    valueStack.push(applyOperator(token, a, b));
                }
            } else if (FUNCTIONS.containsKey(token.toLowerCase())) {
                // 函数求值
                String func = token.toLowerCase();
                int numArgs = FUNCTIONS.get(func);
                if (valueStack.size() < numArgs) {
                    throw new IllegalArgumentException("非法输入: 函数 '" + func + "' 的参数数量不足。");
                }
                List<Double> args = new ArrayList<>();
                for (int i = 0; i < numArgs; i++) {
                    args.add(valueStack.pop());
                }
                Collections.reverse(args); // 参数是逆序出栈的，需要反转回来
                valueStack.push(applyFunction(func, args));
            }
        }

        if (valueStack.size() != 1) {
            throw new IllegalArgumentException("非法输入: 表达式格式错误，导致最终栈中元素不唯一。");
        }

        return valueStack.pop();
    }

    // --- 辅助方法 ---
    private boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isVariable(String token) {
        // 简单定义：不是数字、常数、函数、操作符、括号或逗号的，都视为变量
        return !isNumber(token) && !CONSTANTS.containsKey(token.toLowerCase()) &&
                !FUNCTIONS.containsKey(token.toLowerCase()) && !PRECEDENCE.containsKey(token) &&
                !token.equals("(") && !token.equals(")") && !token.equals(",");
    }

    private double applyOperator(String op, double a, double b) {
        return switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> {
                if (b == 0) throw new ArithmeticException("错误: 除以零。");
                yield a / b;
            }
            case "^" -> Math.pow(a, b);
            default -> throw new IllegalArgumentException("内部错误: 未知的运算符 " + op);
        };
    }

    private double applyFunction(String func, List<Double> args) {
        double a = args.get(0);
        return switch (func.toLowerCase()) {
            case "sin" -> Math.sin(a);
            case "cos" -> Math.cos(a);
            case "tan" -> Math.tan(a);
            case "abs" -> Math.abs(a);
            case "sqrt" -> {
                if (a < 0) throw new ArithmeticException("错误: 不能对负数开平方。");
                yield Math.sqrt(a);
            }
            case "log" -> {
                if (a <= 0) throw new ArithmeticException("错误: log函数的参数必须为正数。");
                yield Math.log(a);
            }
            case "log10" -> {
                if (a <= 0) throw new ArithmeticException("错误: log10函数的参数必须为正数。");
                yield Math.log10(a);
            }
            // 多参数函数
            case "pow" -> Math.pow(a, args.get(1));
            case "max" -> Math.max(a, args.get(1));
            case "min" -> Math.min(a, args.get(1));
            default -> throw new IllegalArgumentException("内部错误: 未知的函数 " + func);
        };
    }
}