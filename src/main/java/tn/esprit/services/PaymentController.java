package tn.esprit.services;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.controllers.TransactionService;
import tn.esprit.entities.Transaction;
import tn.esprit.utils.PaymeeService;
import tn.esprit.utils.SessionManager;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

public class PaymentController implements Initializable {

    @FXML private Label      labelAmount;
    @FXML private Label      labelReceiver;
    @FXML private Label      labelDescription;
    @FXML private Label      labelStatus;
    @FXML private Button     btnOpenBrowser;
    @FXML private Button     btnVerify;
    @FXML private Button     btnConfirmManual;
    @FXML private Button     btnCancel;
    @FXML private VBox       stepVerifyBox;       // section après ouverture navigateur
    @FXML private VBox       stepManualBox;       // section saisie manuelle (fallback)
    @FXML private TextField  txIdField;
    @FXML private ProgressIndicator progressIndicator;

    private final PaymeeService      paymeeService      = new PaymeeService();
    private final TransactionService transactionService = new TransactionService();
    private final SessionManager     session            = SessionManager.getInstance();

    private double  amount;
    private int     receiverId;
    private String  receiverName;
    private String  description;
    private String  type;
    private String  paymeeToken;
    private boolean paymentSuccess = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (stepVerifyBox != null) { stepVerifyBox.setVisible(false); stepVerifyBox.setManaged(false); }
        if (stepManualBox != null) { stepManualBox.setVisible(false); stepManualBox.setManaged(false); }
        if (progressIndicator != null) progressIndicator.setVisible(false);
    }

    public void setPaymentData(double amount, int receiverId, String receiverName,
                               String description, String type,
                               String paymeeToken, String paymentUrl) {
        this.amount       = amount;
        this.receiverId   = receiverId;
        this.receiverName = receiverName;
        this.description  = description;
        this.type         = type;
        this.paymeeToken  = paymeeToken;

        if (labelAmount != null)      labelAmount.setText(String.format("%.3f DT", amount));
        if (labelReceiver != null)    labelReceiver.setText(receiverName);
        if (labelDescription != null) labelDescription.setText(description);
        if (labelStatus != null)      labelStatus.setText("⏳ Prêt. Cliquez sur le bouton pour payer.");

        // Stocker l'URL pour ouvrir le navigateur
        this.paymentUrl = paymentUrl;
    }

    private String paymentUrl;

    // ── Étape 1 : Ouvrir le navigateur ───────────────────────────────────────

    @FXML
    private void openPaymentBrowser() {
        try {
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().browse(new URI(paymentUrl));

            if (btnOpenBrowser != null) {
                btnOpenBrowser.setDisable(true);
                btnOpenBrowser.setText("🌐 Page ouverte dans le navigateur...");
            }
            if (stepVerifyBox != null) { stepVerifyBox.setVisible(true); stepVerifyBox.setManaged(true); }
            if (labelStatus != null)
                labelStatus.setText(
                        "🌐 Page de paiement ouverte.\n\n" +
                                "Connectez-vous : 📱 11111111 / 🔒 11111111\n\n" +
                                "Après paiement, revenez ici et cliquez sur Vérifier."
                );
        } catch (Exception e) {
            showError("Impossible d'ouvrir le navigateur:\n" + e.getMessage());
        }
    }

    // ── Étape 2 : Polling automatique ────────────────────────────────────────

    @FXML
    private void verifyPayment() {
        if (paymeeToken == null) { showError("Token manquant."); return; }

        setLoading(true);
        if (labelStatus != null) labelStatus.setText("🔍 Vérification en cours...");

        Task<PaymeeService.PaymentStatus> task = new Task<>() {
            @Override
            protected PaymeeService.PaymentStatus call() {
                return paymeeService.checkPayment(paymeeToken);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            PaymeeService.PaymentStatus status = task.getValue();

            if (status.isPaid()) {
                // ✅ Polling a fonctionné
                onPaymentConfirmed(status.getTransactionId(), status.getAmount());

            } else if (status.getErrorMessage() != null
                    && status.getErrorMessage().contains("transaction_id")) {
                // Polling ne marche pas en sandbox → afficher saisie manuelle
                showManualFallback();

            } else if (status.getErrorMessage() != null) {
                // Autre erreur
                showManualFallback();
                if (labelStatus != null)
                    labelStatus.setText(
                            "⚠️ Vérification automatique non disponible en sandbox.\n" +
                                    "Saisissez le transaction_id visible dans l'URL du navigateur."
                    );
            } else {
                // Paiement pas encore effectué
                if (labelStatus != null)
                    labelStatus.setText("⏳ Paiement non encore détecté. Payez puis réessayez.");
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            showManualFallback();
        });

        new Thread(task).start();
    }

    // ── Fallback : saisie manuelle sécurisée ─────────────────────────────────

    private void showManualFallback() {
        if (stepManualBox != null) { stepManualBox.setVisible(true); stepManualBox.setManaged(true); }
        if (btnVerify != null) btnVerify.setVisible(false);
        if (labelStatus != null)
            labelStatus.setText(
                    "⚠️ Vérification automatique indisponible en sandbox.\n\n" +
                            "Regardez l'URL du navigateur pendant que les points tournent :\n" +
                            "  ...loader?payment_token=abc&transaction=89569\n\n" +
                            "Entrez le numéro après 'transaction=' ci-dessous."
            );
    }

    @FXML
    private void confirmManual() {
        String text = txIdField != null ? txIdField.getText().trim() : "";
        if (text.isEmpty()) {
            showError("Entrez le numéro de transaction visible dans l'URL.");
            return;
        }
        try {
            int txId = Integer.parseInt(text);
            if (txId <= 0) throw new NumberFormatException();
            // Confirmation manuelle sandbox (transaction_id vient de l'URL Paymee)
            onPaymentConfirmed(txId, amount);
        } catch (NumberFormatException e) {
            showError("Numéro invalide. Entrez seulement les chiffres.\nEx : 89569");
        }
    }

    // ── Paiement confirmé → sauvegarder en DB ────────────────────────────────

    private void onPaymentConfirmed(int transactionId, double confirmedAmount) {
        try {
            Transaction t = new Transaction(
                    session.getCurrentUserId(),
                    receiverId,
                    BigDecimal.valueOf(amount),
                    type,
                    "completed",
                    description,
                    "online",
                    null,
                    "TND"
            );
            transactionService.add(t);
            paymentSuccess = true;

            System.out.println("✅ Transaction DB sauvegardée ! Paymee txId=" + transactionId);

            if (progressIndicator != null) progressIndicator.setVisible(false);
            if (btnCancel != null)         btnCancel.setText("✅ Fermer");
            if (labelStatus != null) {
                labelStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 13px;");
                labelStatus.setText("✅ Paiement confirmé et transaction enregistrée !");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Paiement Réussi !");
            alert.setHeaderText(null);
            alert.setContentText(
                    "✅ Paiement Paymee confirmé !\n\n" +
                            "Montant :         " + String.format("%.3f DT", amount) + "\n" +
                            "Destinataire :    " + receiverName + "\n" +
                            "Transaction ID :  " + transactionId + "\n\n" +
                            "Statut : completed"
            );
            alert.showAndWait();
            closeWindow();

        } catch (Exception e) {
            showError("Paiement reçu mais erreur DB:\n" + e.getMessage());
        }
    }

    // ── Annuler ───────────────────────────────────────────────────────────────

    @FXML
    private void cancel() {
        if (paymentSuccess) { closeWindow(); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler ?");
        confirm.setHeaderText(null);
        confirm.setContentText("La transaction ne sera pas enregistrée.");
        confirm.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) closeWindow(); });
    }

    public boolean isPaymentSuccess() { return paymentSuccess; }

    private void setLoading(boolean on) {
        if (progressIndicator != null) progressIndicator.setVisible(on);
        if (btnVerify != null)         btnVerify.setDisable(on);
    }

    private void closeWindow() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK)
                .showAndWait();
    }
}