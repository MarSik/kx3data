package org.marsik.ham.kx3tool;

import javax.enterprise.util.AnnotationLiteral;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.marsik.ham.kx3tool.cdi.StartupScene;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        final Weld w = new Weld();
        final WeldContainer wc = w.initialize();

        // Make sure the app is instantiated
        wc.event().select(Stage.class, new AnnotationLiteral<StartupScene>() {}).fire(primaryStage);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
