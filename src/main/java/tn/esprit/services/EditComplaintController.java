package tn.esprit.services;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import tn.esprit.controllers.ComplaintService;
import tn.esprit.entities.Complaint;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class EditComplaintController implements Initializable {

    @FXML private TextArea subjectArea;
    @FXML private Label    charCountLabel;
    @FXML private Label    infoLabel;
    @FXML private Label    errorLabel;

    private final ComplaintService complaintService = new ComplaintService();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Data passed from ComplaintController before opening this window ────────
    private static Complaint complaintToEdit;
    private static Runnable  onSaveCallback;

    public static void setData(Complaint complaint, Runnable callback) {
        complaintToEdit  = complaint;
        onSaveCallback   = callback;
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (complaintToEdit == null) return;

        // Populate info strip
        infoLabel.setText(
                "Réclamation #" + complaintToEdit.getId()
                        + "   📅 " + complaintToEdit.getComplaintDate().format(DATE_FORMAT)
                        + "   🏷️ " + complaintToEdit.getStatus().toUpperCase()
        );

        // Pre-fill the subject
        subjectArea.setText(complaintToEdit.getSubject());

        // Char counter
        subjectArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal.length();
            charCountLabel.setText(len + " / 10 caractères minimum");
            charCountLabel.getStyleClass().setAll(
                    len >= 10 ? "form-char-count-ok" : "form-char-count-err"
            );
            errorLabel.setText("");
        });

        // Trigger initial count
        int initial = complaintToEdit.getSubject().length();
        charCountLabel.setText(initial + " / 10 caractères minimum");
        charCountLabel.getStyleClass().setAll(
                initial >= 10 ? "form-char-count-ok" : "form-char-count-err"
        );
    }

    @FXML
    private void saveComplaint() {
        String subject = subjectArea.getText().trim();

        if (subject.isEmpty()) {
            errorLabel.setText("⚠️ Le sujet est obligatoire !");
            return;
        }
        if (subject.length() < 10) {
            errorLabel.setText("⚠️ Le sujet doit contenir au moins 10 caractères !");
            return;
        }

        try {
            complaintToEdit.setSubject(subject);
            complaintService.update(complaintToEdit);
            if (onSaveCallback != null) onSaveCallback.run();
            closeWindow();
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
        Stage stage = (Stage) subjectArea.getScene().getWindow();
        stage.close();
    }
}