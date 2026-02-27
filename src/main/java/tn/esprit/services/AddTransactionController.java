package tn.esprit.services;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.controllers.TransactionService;
import tn.esprit.controllers.UserService;
import tn.esprit.entities.Transaction;
import tn.esprit.entities.User;
import tn.esprit.utils.PaymeeService;
import tn.esprit.utils.SessionManager;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AddTransactionController implements Initializable {

    @FXML private ComboBox<UserItem> receiverCombo;
    @FXML private TextField          amountField;
    @FXML private Label              amountErrorLabel;
    @FXML private TextField          descriptionField;
    @FXML private ToggleButton       debitToggle;
    @FXML private ToggleButton       creditToggle;
    @FXML private ComboBox<String>   statusBox;
    @FXML private VBox               summaryBox;
    @FXML private Label              summaryLabel;
    @FXML private Button             submitButton;
    @FXML private Button             paymeeButton;   // ← NOUVEAU bouton Paymee dans le FXML

    private final TransactionService transactionService = new TransactionService();
    private final UserService        userService        = new UserService();
    private final PaymeeService      paymeeService      = new PaymeeService();
    private final SessionManager     session            = SessionManager.getInstance();
    private final ToggleGroup        typeToggleGroup    = new ToggleGroup();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTypeToggles();
        loadReceivers();
        setupStatusBox();
        setupValidation();
        if (summaryBox != null) {
            summaryBox.setVisible(false);
            summaryBox.setManaged(false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Setup UI
    // ══════════════════════════════════════════════════════════════════════════

    private void setupTypeToggles() {
        debitToggle.setToggleGroup(typeToggleGroup);
        creditToggle.setToggleGroup(typeToggleGroup);

        debitToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal) {
                debitToggle.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 10; " +
                        "-fx-padding: 12 25; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
            } else {
                debitToggle.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 10; " +
                        "-fx-padding: 12 25; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
            }
            updateSummary();
        });

        creditToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal) {
                creditToggle.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 10; " +
                        "-fx-padding: 12 25; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
            } else {
                creditToggle.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 10; " +
                        "-fx-padding: 12 25; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
            }
            updateSummary();
        });
    }

    private void loadReceivers() {
        try {
            List<User> users = userService.getAll();
            if (users != null) {
                for (User u : users) {
                    if (u.getId() != session.getCurrentUserId()) {
                        receiverCombo.getItems().add(new UserItem(u.getId(), u.getName()));
                    }
                }
            }
            receiverCombo.setOnAction(e -> updateSummary());
        } catch (Exception e) {
            System.err.println("Erreur chargement destinataires: " + e.getMessage());
        }
    }

    private void setupStatusBox() {
        statusBox.getItems().addAll("pending", "completed", "failed");
        statusBox.setValue("pending");
        statusBox.setOnAction(e -> updateSummary());
    }

    private void setupValidation() {
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateAmount(newValue);
            updateSummary();
        });
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(oldValue);
            }
        });
        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> updateSummary());
    }

    private void validateAmount(String value) {
        if (amountErrorLabel == null || submitButton == null) return;

        if (value == null || value.isEmpty()) {
            amountErrorLabel.setText("⚠️ Le montant est obligatoire");
            amountErrorLabel.setVisible(true);
            submitButton.setDisable(true);
            if (paymeeButton != null) paymeeButton.setDisable(true);
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                amountErrorLabel.setText("⚠️ Le montant doit être supérieur à 0");
                amountErrorLabel.setVisible(true);
                submitButton.setDisable(true);
                if (paymeeButton != null) paymeeButton.setDisable(true);
            } else if (amount.compareTo(new BigDecimal("1000000")) > 0) {
                amountErrorLabel.setText("⚠️ Montant maximal : 1 000 000 TND");
                amountErrorLabel.setVisible(true);
                submitButton.setDisable(true);
                if (paymeeButton != null) paymeeButton.setDisable(true);
            } else {
                amountErrorLabel.setVisible(false);
                submitButton.setDisable(false);
                if (paymeeButton != null) paymeeButton.setDisable(false);
            }
        } catch (NumberFormatException e) {
            amountErrorLabel.setText("⚠️ Montant invalide");
            amountErrorLabel.setVisible(true);
            submitButton.setDisable(true);
            if (paymeeButton != null) paymeeButton.setDisable(true);
        }
    }

    private void updateSummary() {
        if (summaryBox == null || summaryLabel == null) return;
        if (receiverCombo.getValue() != null &&
                amountField.getText() != null && !amountField.getText().isEmpty() &&
                typeToggleGroup.getSelectedToggle() != null) {
            try {
                BigDecimal amount   = new BigDecimal(amountField.getText());
                String     type     = debitToggle.isSelected() ? "DÉBIT" : "CRÉDIT";
                String     typeIcon = debitToggle.isSelected() ? "💳" : "💰";
                summaryLabel.setText(String.format("%s %s de %.3f TND vers %s",
                        typeIcon, type, amount, receiverCombo.getValue().getName()));
                summaryBox.setVisible(true);
                summaryBox.setManaged(true);
            } catch (Exception e) {
                summaryBox.setVisible(false);
                summaryBox.setManaged(false);
            }
        } else {
            summaryBox.setVisible(false);
            summaryBox.setManaged(false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRANSACTION CLASSIQUE — bouton "Soumettre"
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void addTransaction() {
        if (!validateForm()) return;

        try {
            int        senderId    = session.getCurrentUserId();
            int        receiverId  = receiverCombo.getValue().getId();
            BigDecimal amount      = new BigDecimal(amountField.getText().trim());
            String     type        = debitToggle.isSelected() ? "debit" : "credit";
            String     txStatus    = statusBox.getValue();
            String     description = descriptionField.getText() != null
                    ? descriptionField.getText().trim() : "";

            Transaction t = new Transaction(
                    senderId, receiverId, amount, type, txStatus,
                    description.isEmpty() ? "Transaction " + type : description,
                    "online", null, "TND"
            );

            transactionService.add(t);

            showInfo("✅ Transaction effectuée avec succès !\n\n" +
                    "Expéditeur:   " + session.getCurrentUser().getName() + "\n" +
                    "Destinataire: " + receiverCombo.getValue().getName() + "\n" +
                    "Montant:      " + amount + " TND");

            closeWindow();

        } catch (NumberFormatException e) {
            showError("Le montant doit être un nombre valide !");
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PAIEMENT PAYMEE — bouton "Payer avec Paymee"
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void payWithPaymee() {
        if (!validateForm()) return;

        // Récupérer les données du formulaire
        int    receiverId   = receiverCombo.getValue().getId();
        String receiverName = receiverCombo.getValue().getName();
        double amount       = Double.parseDouble(amountField.getText().trim());
        String type         = debitToggle.isSelected() ? "debit" : "credit";
        String description  = (descriptionField.getText() != null
                && !descriptionField.getText().trim().isEmpty())
                ? descriptionField.getText().trim()
                : "Transaction " + type;

        // Données de l'utilisateur connecté
        String senderName  = session.getCurrentUser().getName();
        String senderEmail = session.getCurrentUser().getEmail() != null
                ? session.getCurrentUser().getEmail() : "user@fintech.tn";
        String senderPhone = session.getCurrentUser().getPhone() != null
                ? session.getCurrentUser().getPhone() : "+21600000000";
        String orderId     = "TXN-" + session.getCurrentUserId() + "-" + System.currentTimeMillis();

        // UI → état loading
        if (paymeeButton != null) {
            paymeeButton.setDisable(true);
            paymeeButton.setText("⏳ Création du paiement Paymee...");
        }
        if (submitButton != null) submitButton.setDisable(true);

        // ── Appel API en background thread ───────────────────────────────────
        Task<PaymeeService.PaymentResult> task = new Task<>() {
            @Override
            protected PaymeeService.PaymentResult call() {
                String[] parts     = senderName.trim().split(" ", 2);
                String   firstName = parts[0];
                String   lastName  = parts.length > 1 ? parts[1] : ".";

                return paymeeService.createPayment(
                        amount, description,
                        firstName, lastName,
                        senderEmail, senderPhone,
                        orderId
                );
            }
        };

        task.setOnSucceeded(event -> {
            // Réactiver les boutons
            if (paymeeButton != null) {
                paymeeButton.setDisable(false);
                paymeeButton.setText("💳 Payer avec Paymee");
            }
            if (submitButton != null) submitButton.setDisable(false);

            PaymeeService.PaymentResult result = task.getValue();

            if (!result.isSuccess()) {
                showError("Erreur Paymee:\n" + result.getErrorMessage());
                return;
            }

            // ✅ Paiement créé → ouvrir la fenêtre de paiement
            openPaymentWindow(
                    result.getToken(), result.getPaymentUrl(),
                    amount, receiverId, receiverName, description, type
            );
        });

        task.setOnFailed(event -> {
            if (paymeeButton != null) {
                paymeeButton.setDisable(false);
                paymeeButton.setText("💳 Payer avec Paymee");
            }
            if (submitButton != null) submitButton.setDisable(false);
            showError("Erreur inattendue:\n" + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Ouvrir la fenêtre PaymentView
    // ══════════════════════════════════════════════════════════════════════════

    private void openPaymentWindow(String token, String paymentUrl,
                                   double amount, int receiverId,
                                   String receiverName, String description, String type) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/PaymentView.fxml")
            );
            Parent root = loader.load();

            PaymentController paymentController = loader.getController();
            paymentController.setPaymentData(
                    amount, receiverId, receiverName, description, type, token, paymentUrl
            );

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("💳 Paiement Paymee — " + String.format("%.3f DT", amount));
            stage.setScene(new Scene(root, 520, 580));
            stage.setResizable(false);
            stage.showAndWait();

            // Après fermeture → si paiement réussi, fermer aussi AddTransaction
            if (paymentController.isPaymentSuccess()) {
                closeWindow();
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur ouverture PaymentView: " + e.getMessage());
            e.printStackTrace();
            showError("Impossible d'ouvrir la fenêtre de paiement:\n" + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Validation formulaire (partagée par les deux boutons)
    // ══════════════════════════════════════════════════════════════════════════

    private boolean validateForm() {
        if (session.getCurrentUser() == null) {
            showError("Aucun utilisateur connecté !");
            return false;
        }
        if (receiverCombo.getValue() == null) {
            showError("Veuillez sélectionner un destinataire !");
            return false;
        }
        if (amountField.getText() == null || amountField.getText().trim().isEmpty()) {
            showError("Le montant est obligatoire !");
            return false;
        }
        if (typeToggleGroup.getSelectedToggle() == null) {
            showError("Veuillez sélectionner le type (Débit ou Crédit) !");
            return false;
        }
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Le montant doit être supérieur à 0 !");
                return false;
            }
            if (session.getCurrentUserId() == receiverCombo.getValue().getId()) {
                showError("Vous ne pouvez pas faire une transaction vers vous-même !");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Le montant doit être un nombre valide !");
            return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Utilitaires
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText(null);
        alert.setTitle("Succès");
        alert.showAndWait();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.setHeaderText("Erreur");
        alert.setTitle("Erreur");
        alert.showAndWait();
    }

    // ── Classe interne UserItem ───────────────────────────────────────────────
    public static class UserItem {
        private final int    id;
        private final String name;

        public UserItem(int id, String name) {
            this.id   = id;
            this.name = name;
        }

        public int    getId()   { return id;   }
        public String getName() { return name; }

        @Override
        public String toString() { return name + " (ID: " + id + ")"; }
    }
}