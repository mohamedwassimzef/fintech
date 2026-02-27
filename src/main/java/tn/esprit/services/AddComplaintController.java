package tn.esprit.services;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.controllers.ComplaintService;
import tn.esprit.entities.Complaint;
import tn.esprit.utils.ContentModerationService;
import tn.esprit.utils.ContentModerationService.ModerationResult;
import tn.esprit.utils.SessionManager;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Controller for AddComplaintView.fxml
 *
 * Validation pipeline:
 *   Step 1 — Field not empty (local, instant)
 *   Step 2 — Minimum 10 characters (local, instant)
 *   Step 3 — HuggingFace AI moderation API (async background thread)
 *   Step 4 — Save to database
 */
public class AddComplaintController implements Initializable {

    @FXML private TextArea subjectArea;
    @FXML private Label    charCountLabel;
    @FXML private Button   submitButton;

    private final ComplaintService         complaintService  = new ComplaintService();
    private final ContentModerationService moderationService = new ContentModerationService();
    private final SessionManager           session           = SessionManager.getInstance();

    // ──────────────────────────────────────────────────────────────────────────
    //  Init
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        subjectArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal.length();
            charCountLabel.setText(len + " / 10 caractères minimum");
            charCountLabel.getStyleClass().setAll(
                    len >= 10 ? "form-char-count-ok" : "form-char-count-err"
            );
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Submit — called when user clicks "Soumettre"
    // ──────────────────────────────────────────────────────────────────────────

    @FXML
    private void addComplaint() {
        String subject = subjectArea.getText().trim();

        // Step 1 & 2: Local validation (instant, no API call)
        if (subject.isEmpty()) {
            showError("Le sujet est obligatoire !");
            return;
        }
        if (subject.length() < 10) {
            showError("Le sujet doit contenir au moins 10 caractères !");
            return;
        }

        // Step 3: API moderation (async so UI doesn't freeze)
        runModerationThenSave(subject);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Background moderation task
    // ──────────────────────────────────────────────────────────────────────────

    private void runModerationThenSave(String subject) {

        // Disable form while checking (prevents double-click)
        setUiEnabled(false);
        if (submitButton != null) submitButton.setText("🔍 Vérification en cours...");

        // JavaFX Task = background job that can safely update the UI when done
        Task<ModerationResult> task = new Task<>() {
            @Override
            protected ModerationResult call() {
                // Runs on background thread — safe to do slow HTTP here
                // moderationService.analyse() may throw RuntimeException if:
                //   - token not configured
                //   - model loading (503)
                //   - network error
                return moderationService.analyse(subject);
            }
        };

        // ── Called on JavaFX thread when moderation completes successfully ────
        task.setOnSucceeded(e -> {
            ModerationResult result = task.getValue();

            if (result.isFlagged()) {
                // API says content is toxic — show reason and block
                setUiEnabled(true);
                if (submitButton != null) submitButton.setText("✅ Soumettre");
                showModerationWarning(result);
            } else {
                // API says content is clean — save to database
                saveComplaint(subject);
            }
        });

        // ── Called on JavaFX thread if moderationService.analyse() throws ─────
        // This happens when: token missing, model loading, network error, etc.
        task.setOnFailed(e -> {
            setUiEnabled(true);
            if (submitButton != null) submitButton.setText("✅ Soumettre");

            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Erreur inconnue lors de la modération.";
            showError(msg);
        });

        // Run in a daemon thread (auto-stops when app closes)
        Thread thread = new Thread(task, "moderation-thread");
        thread.setDaemon(true);
        thread.start();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Save to database (only called after moderation passes)
    // ──────────────────────────────────────────────────────────────────────────

    private void saveComplaint(String subject) {
        try {
            Complaint complaint = new Complaint(
                    subject,
                    "pending",
                    LocalDate.now(),
                    session.getCurrentUserId()
            );
            complaintService.add(complaint);

            if (submitButton != null) submitButton.setText("✅ Soumettre");
            showInfo("✅ Réclamation ajoutée avec succès !\n\nVous serez notifié dès qu'elle sera traitée.");
            closeWindow();

        } catch (Exception e) {
            setUiEnabled(true);
            if (submitButton != null) submitButton.setText("✅ Soumettre");
            showError("Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  UI helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void setUiEnabled(boolean enabled) {
        subjectArea.setDisable(!enabled);
        if (submitButton != null) submitButton.setDisable(!enabled);
    }

    private void showModerationWarning(ModerationResult result) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Contenu inapproprié détecté");
        alert.setHeaderText("⚠️ Votre réclamation ne peut pas être soumise");
        alert.setContentText(result.getReason());
        alert.getDialogPane().setMinWidth(500);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.setTitle("Succès");
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText("Erreur");
        a.setTitle("Erreur");
        a.showAndWait();
    }

    @FXML private void cancel() { closeWindow(); }

    private void closeWindow() {
        ((Stage) subjectArea.getScene().getWindow()).close();
    }
}