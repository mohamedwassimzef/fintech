package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.controllers.UserService;
import tn.esprit.entities.User;
import tn.esprit.utils.SessionManager;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {

            // ✅ 1) Initialiser la session utilisateur
            initializeSession();

            // ✅ 2) Charger la vue principale
            Parent root = FXMLLoader.load(
                    getClass().getResource("/TransactionView.fxml")
            );

            // ✅ 3) Créer la scène
            Scene scene = new Scene(root, 1200, 700);

            primaryStage.setTitle("FINTECH - Gestion Financière");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("❌ Erreur au démarrage de l'application");
            e.printStackTrace();
        }
    }

    /**
     * Initialise la session avec un utilisateur de test
     * ⚠️ CHANGEZ L'ID selon votre base de données
     */
    private void initializeSession() {

        UserService userService = new UserService();

        // 🔧 CONFIGURATION
        // ID = 1 → Admin
        // ID = 2+ → Utilisateur normal
        int TEST_USER_ID = 6; // ← CHANGEZ ICI si besoin

        User testUser = userService.getById(TEST_USER_ID);

        if (testUser != null) {
            SessionManager.getInstance().setCurrentUser(testUser);

            System.out.println("✅ Session initialisée avec : "
                    + testUser.getName()
                    + " (Role ID: "
                    + testUser.getRoleId() + ")");
        } else {
            System.err.println("❌ Aucun utilisateur trouvé avec ID = " + TEST_USER_ID);
            System.err.println("Vérifiez votre base de données.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
