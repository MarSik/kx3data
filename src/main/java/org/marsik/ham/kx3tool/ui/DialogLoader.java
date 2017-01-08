package org.marsik.ham.kx3tool.ui;

import javax.inject.Inject;

import java.io.IOException;
import java.io.InputStream;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.marsik.ham.kx3tool.cdi.FXMLLoaderProducer;

public class DialogLoader {
    @Inject
    FXMLLoaderProducer loaderProducer;

    public <T> Dialog<T> showDialog(Stage parent, String fxml, String title) {
        final InputStream is;
        final Parent root;
        final FXMLLoader loader;

        try {
            is = getClass().getClassLoader().getResourceAsStream(fxml);
            loader = loaderProducer.createLoader();
            root = loader.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load dialog", e);
        }

        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(parent);

        Scene dialogScene = new Scene(root);
        dialogStage.setScene(dialogScene);

        return new Dialog<T>(dialogStage, loader.getController());
    }
}
