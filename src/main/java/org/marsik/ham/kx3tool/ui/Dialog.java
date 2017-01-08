package org.marsik.ham.kx3tool.ui;

import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Dialog<T> {
    Stage stage;
    T controller;

    public void showAndWait() {
        stage.showAndWait();
    }
}
