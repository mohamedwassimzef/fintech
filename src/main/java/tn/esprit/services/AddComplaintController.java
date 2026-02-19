package tn.esprit.services;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.controllers.ComplaintService;
import tn.esprit.entities.Complaint;
import tn.esprit.utils.SessionManager;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AddComplaintController implements Initializable {

    @FXML private TextArea subjectArea;
    @FXML private Label    charCountLabel;

    private final ComplaintService complaintService = new ComplaintService();
    private final SessionManager   session          = SessionManager.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        subjectArea.textProperty().addListener((observable, oldValue, newValue) -> {
            int len = newValue.length();
            charCountLabel.setText(len + " / 10 caractères minimum");
            // Swap CSS class — no inline setStyle
            charCountLabel.getStyleClass().setAll(
                    len >= 10 ? "form-char-count-ok" : "form-char-count-err"
            );
        });
    }

    @FXML
    private void addComplaint() {
        String subject = subjectArea.getText().trim();

        if (subject.isEmpty()) {
            showError("Le sujet est obligatoire !");
            return;
        }
        if (subject.length() < 10) {
            showError("Le sujet doit contenir au moins 10 caractères !");
            return;
        }

        try {
            Complaint complaint = new Complaint(
                    subject,
                    "pending",
                    LocalDate.now(),
                    session.getCurrentUserId()
            );
            complaintService.add(complaint);
            showInfo("✅ Réclamation ajoutée avec succès !\n\nVous serez notifié dès qu'elle sera traitée.");
            closeWindow();
        } catch (Exception e) {
            showError("Erreur lors de l'ajout : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void cancel() { closeWindow(); }

    private void closeWindow() {
        ((Stage) subjectArea.getScene().getWindow()).close();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null); a.setTitle("Succès"); a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText("Erreur"); a.setTitle("Erreur"); a.showAndWait();
    }
}