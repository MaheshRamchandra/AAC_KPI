package com.aac.kpi;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;

public final class SplashScreen {
    private SplashScreen() {}

    public static void play(Stage stage, Runnable onFinished) {
        // Try to load the KPI logo from resources first, then fallback to project root file
        Image logo = null;
        try (InputStream is = SplashScreen.class.getResourceAsStream("/com/aac/kpi/KPILogo.png")) {
            if (is != null) {
                logo = new Image(is);
            }
        } catch (Exception ignored) {}
        if (logo == null) {
            try {
                logo = new Image("file:KPILogo.png");
            } catch (Exception ignored) {}
        }

        ImageView iv = new ImageView();
        if (logo != null && !logo.isError()) {
            iv.setImage(logo);
            iv.setPreserveRatio(true);
            iv.setFitWidth(420);
        }

        Label fallback = new Label("KPI");
        fallback.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
        // Root with violet gradient background (replace previous blue)
        StackPane root = new StackPane(logo != null && !logo.isError() ? iv : fallback);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #6a00ff, #b517ff);");

        // Remove side stripes to avoid covering the logo; keep clean violet background only

        Scene splashScene = new Scene(root, 640, 420, Color.TRANSPARENT);
        stage.setScene(splashScene);
        stage.setTitle("KPI");
        stage.centerOnScreen();
        stage.show();

        // Compose a simple intro animation: fade + gentle zoom
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        ScaleTransition zoomIn = new ScaleTransition(Duration.millis(900), root);
        zoomIn.setFromX(0.96);
        zoomIn.setFromY(0.96);
        zoomIn.setToX(1.04);
        zoomIn.setToY(1.04);

        PauseTransition hold = new PauseTransition(Duration.millis(550));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition seq = new SequentialTransition(
                new ParallelTransition(fadeIn, zoomIn),
                hold,
                fadeOut
        );
        seq.setOnFinished(e -> {
            try {
                if (onFinished != null) onFinished.run();
            } finally {
                root.setOpacity(1.0);
            }
        });
        seq.play();
    }
}
