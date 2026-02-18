package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the Welcome Screen (changed from Dashboard)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WelcomeScreen.fxml"));
            Parent root = loader.load();

            // Create the scene
            Scene scene = new Scene(root, 1100, 750);

            // Set up the stage
            primaryStage.setTitle("Personal Finance Manager");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error loading Welcome Screen: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}