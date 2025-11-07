// 文件路径: src/com/twx/linear_systems/view/MatrixView.java
package com.twx.linear_systems.view;

import com.twx.linear_systems.model.MatrixState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.Arrays;

public class MatrixView extends GridPane {

    public MatrixView() {
        this.getStyleClass().add("matrix-view");
        this.setPadding(new Insets(10));
        this.setHgap(10);
        this.setVgap(10);
        this.setAlignment(Pos.CENTER);
    }

    public void updateMatrix(MatrixState state) {
        this.getChildren().clear();
        RealMatrix matrix = state.matrix();
        int[] highlightedRows = state.highlightedRows() != null ? state.highlightedRows() : new int[0];

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                Label label = new Label(String.format("%.2f", matrix.getEntry(i, j)));
                label.setPadding(new Insets(5));
                label.setStyle("-fx-font-size: 14px;");

                final int currentRow = i;
                boolean isHighlighted = Arrays.stream(highlightedRows).anyMatch(hRow -> hRow == currentRow);

                if (isHighlighted) {
                    label.setBackground(new Background(new BackgroundFill(Color.LIGHTYELLOW, CornerRadii.EMPTY, Insets.EMPTY)));
                }
                if (j == matrix.getColumnDimension() - 1) { // Augmented part
                    label.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, new BorderWidths(0, 0, 0, 1))));
                    label.setPadding(new Insets(5, 5, 5, 15));
                }

                this.add(label, j, i);
            }
        }
    }
}