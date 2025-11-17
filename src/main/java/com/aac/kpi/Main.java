package com.aac.kpi;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Show splash animation with KPI logo, then load the main UI
        SplashScreen.play(stage, () -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/aac/kpi/MainView.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1200, 700);
                stage.setTitle("KPI Data Generator");
                stage.setScene(scene);
                stage.centerOnScreen();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
