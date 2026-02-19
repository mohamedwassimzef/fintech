package tn.esprit.services;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.controllers.TransactionService;
import tn.esprit.entities.Transaction;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class EditTransactionController implements Initializable {

    @FXML private TextField      amountField;
    @FXML private TextField      descriptionField;
    @FXML private ComboBox<String> statusBox;
    @FXML private Label          infoLabel;
    @FXML private Label          errorLabel;

    private final TransactionService transactionService = new TransactionService();

    // ── Data passed from TransactionController before opening this window ──────
    private static Transaction transactionToEdit;
    private static Runnable    onSaveCallback;

    public static void setData(Transaction transaction, Runnable callback) {
        transactionToEdit = transaction;
        onSaveCallback    = callback;
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (transactionToEdit == null) return;

        boolean isDebit = "debit".equalsIgnoreCase(transactionToEdit.getType());

        // Info strip
        infoLabel.setText(
                (isDebit ? "💳 DÉBIT" : "💰 CRÉDIT")
                        + "   📤 " + transactionToEdit.getSenderName()
                        + "  →  📥 " + transactionToEdit.getReceiverName()
        );

        // Pre-fill fields
        amountField.setText(transactionToEdit.getAmount().toPlainString());
        descriptionField.setText(
                transactionToEdit.getDescription() != null ? transactionToEdit.getDescription() : ""
        );

        statusBox.getItems().addAll("pending", "completed", "failed");
        statusBox.setValue(transactionToEdit.getStatus());

        // Live amount validation
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(oldVal);
            }
            errorLabel.setText("");
        });
    }

    @FXML
    private void saveTransaction() {
        String amountText = amountField.getText().trim();

        if (amountText.isEmpty()) {
            errorLabel.setText("⚠️ Le montant est obligatoire !");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountText);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                errorLabel.setText("⚠️ Le montant doit être supérieur à 0 !");
                return;
            }
            if (amount.compareTo(new BigDecimal("1000000")) > 0) {
                errorLabel.setText("⚠️ Le montant ne peut pas dépasser 1 000 000 TND !");
                return;
            }

            transactionToEdit.setAmount(amount);
            transactionToEdit.setDescription(descriptionField.getText().trim());
            transactionToEdit.setStatus(statusBox.getValue());

            transactionService.update(transactionToEdit);
            if (onSaveCallback != null) onSaveCallback.run();
            closeWindow();

        } catch (NumberFormatException e) {
            errorLabel.setText("⚠️ Montant invalide !");
        } catch (Exception e) {
            errorLabel.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }
}