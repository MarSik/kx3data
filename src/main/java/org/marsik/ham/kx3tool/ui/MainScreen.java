package org.marsik.ham.kx3tool.ui;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import java.io.IOException;
import java.io.InputStream;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.marsik.ham.kx3tool.cdi.FXMLLoaderProducer;
import org.marsik.ham.kx3tool.cdi.StartupScene;

public class MainScreen {
    @Inject
    FXMLLoader loader;

    public void launchJavaFXApplication(@Observes @StartupScene Stage s) {
        InputStream is = null;

        try {
            is = getClass().getClassLoader().getResourceAsStream("test.fxml");
            // we just load our FXML form (including controler and so on)
            Parent root = loader.load(is);
            s.setScene(new Scene(root, 700, 400));
            s.setTitle("KX3 terminal");
            s.show(); // let's show the scene
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load the main screen", e);
        }
    }
}
