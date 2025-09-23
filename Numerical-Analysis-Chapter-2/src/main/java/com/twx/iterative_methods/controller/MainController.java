package com.twx.iterative_methods.controller;

import com.twx.iterative_methods.model.*;
import com.twx.iterative_methods.model.impl.*;
import com.twx.iterative_methods.view.OneDimPlot;
import com.twx.iterative_methods.view.TwoDimPlot;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class MainController {

    // FXML Controls
    @FXML private TextField fField, gField, iterationsField, initialValueField, secondInitialValueField;
    @FXML private Label gLabel;
    @FXML private ComboBox<String> methodComboBox;
    @FXML private Button startButton, clearButton;
    @FXML private TwoDimPlot twoDimPlot; // This is now the Pane that manages its own layers
    @FXML private OneDimPlot oneDimPlot;
    @FXML private HBox x1Container;
    @FXML private TextArea logArea;

    // Member Variables
    private Equation currentEquation;
    private String currentFString = "";
    private Timeline iterationTimeline;
    private final IterativeMethod[] methods = {
            new SimpleIterationMethod(), new NewtonMethod(), new SteffensenMethod(),
            new SimplifiedNewtonMethod(), new ModifiedSecantMethod(), new DampedNewtonMethod(),
            new SinglePointSecantMethod(), new SecantMethod()
    };
    private final String simpleIterationName = new SimpleIterationMethod().getName();
    private final String secantMethodName = new SecantMethod().getName();

    @FXML
    public void initialize() {
        // --- THIS IS THE CORRECTED SECTION ---
        // The FXML loader has already placed the TwoDimPlot pane correctly.
        // We no longer need to manually create a StackPane or re-parent anything.
        // The structure from FXML is: VBox -> TwoDimPlot (Pane) -> [Canvas layers]

        // Initialize UI controls
        methodComboBox.setItems(FXCollections.observableArrayList(
                Stream.of(methods).map(IterativeMethod::getName).toList()
        ));
        methodComboBox.getSelectionModel().select(1); // Default to Newton's method

        // Add listener for smart UI changes
        methodComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateUiForSelectedMethod());
        updateUiForSelectedMethod(); // Initial call to set the UI correctly

        // Set button actions
        startButton.setOnAction(e -> startAnimation());
        clearButton.setOnAction(e -> {
            if (iterationTimeline != null) iterationTimeline.stop();
            twoDimPlot.clearIterations();
            oneDimPlot.clearMappings();
            logArea.clear();
        });

        // Perform initial draw
        twoDimPlot.drawAllLayers(null, null);
        oneDimPlot.drawBase();
    }

    private void startAnimation() {
        if (iterationTimeline != null) iterationTimeline.stop();
        twoDimPlot.clearIterations();
        oneDimPlot.clearMappings();
        logArea.clear();

        Equation newEquation;
        int iterations;
        double x0;
        IterativeMethod selectedMethod = methods[methodComboBox.getSelectionModel().getSelectedIndex()];

        try {
            String gStr = selectedMethod.getName().equals(simpleIterationName) ? gField.getText() : "";
            newEquation = new Equation(fField.getText(), gStr);
            iterations = Integer.parseInt(iterationsField.getText());
            x0 = Double.parseDouble(initialValueField.getText());
        } catch (Exception e) {
            showError("Input Invalid", "Please check the function expressions or parameters.\nError: " + e.getMessage());
            return;
        }

        List<Double> sequence = generateSequence(selectedMethod, newEquation, x0, iterations);
        if (sequence.size() < 2) {
            showError("Calculation Error", "Could not generate iteration sequence. Please check initial values and functions.");
            return;
        }

        boolean functionChanged = !fField.getText().equals(currentFString);
        double[] newBounds = calculateAxisBounds(newEquation, sequence);
        boolean rangeChanged = hasRangeChangedSignificantly(newBounds);

        if (functionChanged || rangeChanged) {
            currentEquation = newEquation;
            currentFString = fField.getText();
            oneDimPlot.animateAndDrawBase();
            twoDimPlot.animateToNewRange(newBounds[0], newBounds[1], newBounds[2], newBounds[3],
                    currentEquation.getF(), currentEquation.getG(),
                    () -> startIterationAnimation(sequence, currentEquation, selectedMethod));
        } else {
            startIterationAnimation(sequence, currentEquation, selectedMethod);
        }
    }

    private void startIterationAnimation(List<Double> sequence, Equation equation, IterativeMethod method) {
        logArea.appendText(String.format("%-4s | %-22s | %-22s\n", "k", "x_k", "f(x_k)"));
        logArea.appendText("-".repeat(52) + "\n");

        iterationTimeline = new Timeline();
        for (int i = 0; i < sequence.size() - 1; i++) {
            final int step = i;
            KeyFrame kf = new KeyFrame(Duration.seconds(step * 0.8), e -> {
                double x_n = sequence.get(step);
                double x_n1 = sequence.get(step + 1);

                double fx = equation.getF().apply(x_n);
                logArea.appendText(String.format("%-4d | %-22.15f | %-22.15f\n", step, x_n, fx));

                method.draw2DStep(twoDimPlot.getIterationContext(), equation, x_n, x_n1, twoDimPlot);
                oneDimPlot.drawMapping(x_n, x_n1, getColorForMethod(method));
            });
            iterationTimeline.getKeyFrames().add(kf);
        }

        int lastStep = sequence.size() - 1;
        iterationTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(lastStep * 0.8), e -> {
            double last_x = sequence.get(lastStep);
            double last_fx = equation.getF().apply(last_x);
            logArea.appendText(String.format("%-4d | %-22.15f | %-22.15f\n", lastStep, last_x, last_fx));
        }));

        iterationTimeline.play();
    }

    // --- Helper Methods (unchanged from the fixed version) ---

    private double[] calculateAxisBounds(Equation eq, List<Double> sequence) {
        double minX = sequence.stream().min(Double::compare).orElse(0.0);
        double maxX = sequence.stream().max(Double::compare).orElse(1.0);
        double paddingX = (maxX - minX) * 0.2;
        paddingX = Math.max(paddingX, 1.0);

        double finalMinX = minX - paddingX;
        double finalMaxX = maxX + paddingX;

        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        int samples = 200;
        for (int i = 0; i <= samples; i++) {
            double x = finalMinX + i * (finalMaxX - finalMinX) / samples;
            try {
                double valF = eq.getF().apply(x);
                if (Double.isFinite(valF)) {
                    if (valF < minY) minY = valF;
                    if (valF > maxY) maxY = valF;
                }
                double valG = eq.getG().apply(x);
                if (Double.isFinite(valG)) {
                    if (valG < minY) minY = valG;
                    if (valG > maxY) maxY = valG;
                }
            } catch (Exception ignored) {}
        }

        minY = Math.min(minY, finalMinX);
        maxY = Math.max(maxY, finalMaxX);

        double paddingY = (maxY - minY) * 0.15;
        paddingY = Math.max(paddingY, 1.0);

        if (!Double.isFinite(minY) || !Double.isFinite(maxY)) {
            return new double[]{-5, 5, -5, 5};
        }

        return new double[]{finalMinX, finalMaxX, minY - paddingY, maxY + paddingY};
    }

    private boolean hasRangeChangedSignificantly(double[] newBounds) {
        if (currentEquation == null) return true;

        double oldWidth = twoDimPlot.getXMax() - twoDimPlot.getXMin();
        double newWidth = newBounds[1] - newBounds[0];
        if (Math.abs(oldWidth) < 1e-9 || Math.abs(newWidth) < 1e-9) return true;
        if (Math.abs(1 - newWidth / oldWidth) > 0.5) return true;

        double oldCenter = (twoDimPlot.getXMax() + twoDimPlot.getXMin()) / 2;
        double newCenter = (newBounds[1] + newBounds[0]) / 2;
        return Math.abs(oldCenter - newCenter) / oldWidth > 0.5;
    }

    private void updateUiForSelectedMethod() {
        String selected = methodComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        boolean isSimple = selected.equals(simpleIterationName);
        gLabel.setVisible(isSimple); gField.setVisible(isSimple);
        gLabel.setManaged(isSimple); gField.setManaged(isSimple);
        boolean isSecant = selected.equals(secantMethodName);
        x1Container.setVisible(isSecant); x1Container.setManaged(isSecant);
    }
    private List<Double> generateSequence(IterativeMethod method, Equation eq, double x0, int iterations) {
        if (method instanceof SecantMethod) {
            try {
                double x1 = Double.parseDouble(secondInitialValueField.getText());
                return ((SecantMethod) method).generateSequence(eq, x0, x1, iterations);
            } catch (NumberFormatException e) {
                showError("Invalid Input", "Please enter a valid initial value for x1.");
                return List.of();
            }
        }
        return method.generateSequence(eq, x0, iterations);
    }
    private Color getColorForMethod(IterativeMethod method) {
        if (method instanceof NewtonMethod) return Color.BLUE;
        if (method instanceof SteffensenMethod) return Color.GREEN;
        if (method instanceof SimplifiedNewtonMethod) return Color.rgb(0, 100, 100);
        if (method instanceof ModifiedSecantMethod) return Color.rgb(255, 140, 0);
        if (method instanceof DampedNewtonMethod) return Color.rgb(75, 0, 130);
        if (method instanceof SinglePointSecantMethod) return Color.ORANGE;
        if (method instanceof SecantMethod) return Color.PURPLE;
        return Color.RED;
    }
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}