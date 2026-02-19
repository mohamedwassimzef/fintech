package tn.esprit.services;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.controllers.TransactionService;
import tn.esprit.controllers.UserService;
import tn.esprit.entities.Transaction;
import tn.esprit.entities.User;
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

    private final TransactionService transactionService = new TransactionService();
    private final UserService        userService        = new UserService();
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

    // ── Toggle buttons ─────────────────────────────────────────────────────────

    private void setupTypeToggles() {
        debitToggle.setToggleGroup(typeToggleGroup);
        creditToggle.setToggleGroup(typeToggleGroup);

        // Use CSS :selected pseudo-class — no inline setStyle needed.
        // The toggle-debit:selected and toggle-credit:selected rules in buttons.css handle color.
        debitToggle.selectedProperty().addListener((obs, oldVal, newVal) -> updateSummary());
        creditToggle.selectedProperty().addListener((obs, oldVal, newVal) -> updateSummary());
    }

    // ── Receivers ──────────────────────────────────────────────────────────────

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
            System.err.println("Erreur chargement destinataires : " + e.getMessage());
        }
    }

    // ── Status box ─────────────────────────────────────────────────────────────

    private void setupStatusBox() {
        statusBox.getItems().addAll("pending", "completed", "failed");
        statusBox.setValue("pending");
        statusBox.setOnAction(e -> updateSummary());
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    private void setupValidation() {
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(oldVal);
            }
            validateAmount(amountField.getText());
            updateSummary();
        });
        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> updateSummary());
    }

    private void validateAmount(String value) {
        if (amountErrorLabel == null || submitButton == null) return;

        if (value == null || value.isEmpty()) {
            amountErrorLabel.setText("⚠️ Le montant est obligatoire");
            amountErrorLabel.setVisible(true);
            submitButton.setDisable(true);
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                amountErrorLabel.setText("⚠️ Le montant doit être supérieur à 0");
                amountErrorLabel.setVisible(true);
                submitButton.setDisable(true);
            } else if (amount.compareTo(new BigDecimal("1000000")) > 0) {
                amountErrorLabel.setText("⚠️ Le montant ne peut pas dépasser 1 000 000 TND");
                amountErrorLabel.setVisible(true);
                submitButton.setDisable(true);
            } else {
                amountErrorLabel.setVisible(false);
                submitButton.setDisable(false);
            }
        } catch (NumberFormatException e) {
            amountErrorLabel.setText("⚠️ Montant invalide");
            amountErrorLabel.setVisible(true);
            submitButton.setDisable(true);
        }
    }

    // ── Summary preview ────────────────────────────────────────────────────────

    private void updateSummary() {
        if (summaryBox == null || summaryLabel == null) return;

        if (receiverCombo.getValue() != null
                && amountField.getText() != null
                && !amountField.getText().isEmpty()
                && typeToggleGroup.getSelectedToggle() != null) {
            try {
                BigDecimal amount   = new BigDecimal(amountField.getText());
                boolean    isDebit  = debitToggle.isSelected();
                String     typeIcon = isDebit ? "💳" : "💰";
                String     typeName = isDebit ? "DÉBIT" : "CRÉDIT";

                summaryLabel.setText(String.format(
                        "%s %s de %.2f TND vers %s",
                        typeIcon, typeName, amount, receiverCombo.getValue().getName()
                ));
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

    // ── Submit ─────────────────────────────────────────────────────────────────

    @FXML
    private void addTransaction() {
        if (session.getCurrentUser() == null) { showError("Aucun utilisateur connecté !"); return; }
        if (receiverCombo.getValue() == null)  { showError("Veuillez sélectionner un destinataire !"); return; }
        if (amountField.getText() == null || amountField.getText().trim().isEmpty()) {
            showError("Le montant est obligatoire !"); return;
        }
        if (typeToggleGroup.getSelectedToggle() == null) {
            showError("Veuillez sélectionner le type (Débit ou Crédit) !"); return;
        }

        try {
            int        senderId   = session.getCurrentUserId();
            int        receiverId = receiverCombo.getValue().getId();

            if (senderId == receiverId) {
                showError("Vous ne pouvez pas faire une transaction vers vous-même !"); return;
            }

            BigDecimal amount      = new BigDecimal(amountField.getText().trim());
            String     type        = debitToggle.isSelected() ? "debit" : "credit";
            String     status      = statusBox.getValue();
            String     description = descriptionField.getText() != null
                    ? descriptionField.getText().trim() : "";

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Le montant doit être supérieur à 0 !"); return;
            }

            Transaction t = new Transaction(
                    senderId, receiverId, amount, type, status,
                    description.isEmpty() ? "Transaction " + type : description,
                    "online", null, "TND"
            );

            transactionService.add(t);
            showInfo("✅ Transaction effectuée avec succès !\n\n"
                    + "Expéditeur: "  + session.getCurrentUser().getName() + "\n"
                    + "Destinataire: " + receiverCombo.getValue().getName() + "\n"
                    + "Montant: "     + amount + " TND");
            closeWindow();

        } catch (NumberFormatException e) {
            showError("Le montant doit être un nombre valide !");
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void cancel() { closeWindow(); }

    private void closeWindow() {
        ((Stage) amountField.getScene().getWindow()).close();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null); a.setTitle("Succès"); a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText("Erreur"); a.setTitle("Erreur"); a.showAndWait();
    }

    // ── Inner class ────────────────────────────────────────────────────────────

    public static class UserItem {
        private final int    id;
        private final String name;

        public UserItem(int id, String name) { this.id = id; this.name = name; }
        public int    getId()   { return id;   }
        public String getName() { return name; }

        @Override
        public String toString() { return name + " (ID: " + id + ")"; }
    }
}