package com.twx;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.Objects;

public class MainViewController {

    @FXML
    private ChoiceBox<String> moduleChooser;

    @FXML
    private BorderPane contentPane;

    private final String NON_LINEAR_EQUATIONS = "非线性方程迭代解法";
    private final String LINEAR_SYSTEMS = "线性方程组求解";

    private Node iterativeMethodsView;
    private Node linearSystemsView;

    @FXML
    public void initialize() throws IOException {
        // 预加载两个模块的视图
        iterativeMethodsView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/twx/iterative_methods/iterative-methods-view.fxml")));
        linearSystemsView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/twx/linear_systems/linear-system-view.fxml")));

        moduleChooser.setItems(FXCollections.observableArrayList(NON_LINEAR_EQUATIONS, LINEAR_SYSTEMS));
        moduleChooser.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                switch (newVal) {
                    case NON_LINEAR_EQUATIONS:
                        contentPane.setCenter(iterativeMethodsView);
                        break;
                    case LINEAR_SYSTEMS:
                        contentPane.setCenter(linearSystemsView);
                        break;
                }
            }
        });

        // 默认选择第一个模块
        moduleChooser.getSelectionModel().selectFirst();
    }
}