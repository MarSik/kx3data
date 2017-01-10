package org.marsik.ham.kx3tool.ui;


import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.marsik.ham.kx3tool.configuration.Macro;

public class EditMacroController implements Initializable {
    @FXML private TextField macroName;
    @FXML private TextArea macroContent;

    private boolean ok = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void load(Macro macro) {
        macroName.setText(macro.getName());
        macroContent.setText(macro.getValue());
    }

    public void onOkPressed(MouseEvent event) {
        ok = !macroName.getText().isEmpty();
        if (ok) {
            close((Node) event.getSource());
        } else {
            macroName.requestFocus();
        }
    }

    public void onCancelPressed(MouseEvent event) {
        close((Node)event.getSource());
    }

    private void close(Node node) {
        ((Stage)node.getScene().getWindow()).close();
    }

    public boolean isOk() {
        return ok;
    }

    public Macro getValue() {
        return new Macro(macroName.getText(), macroContent.getText());
    }
}
