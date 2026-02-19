package tn.esprit.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.controllers.ComplaintService;
import tn.esprit.controllers.UserService;
import tn.esprit.entities.Complaint;
import tn.esprit.utils.EmailService;
import tn.esprit.utils.PdfExportService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ComplaintController implements Initializable {

    @FXML private VBox complaintsContainer;
    @FXML private Label labelPending;
    @FXML private Label labelResolved;
    @FXML private Label labelRejected;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelUserRole;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button btnNewComplaint;
    @FXML private Button btnTransactions;

    private final ComplaintService complaintService = new ComplaintService();
    private final UserService userService = new UserService();
    private final PdfExportService pdfExportService = new PdfExportService();
    private final EmailService emailService = new EmailService();
    private final ObservableList<Complaint> allComplaints = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final SessionManager session = SessionManager.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!session.isLoggedIn()) {
            showError("Aucun utilisateur connecté !");
            return;
        }

        setupUI();
        initStatusFilter();
        displayCurrentUser();
        refreshComplaints();
    }

    private void setupUI() {
        if (!session.isAdmin()) {
            if (btnNewComplaint != null) btnNewComplaint.setVisible(true);
        } else {
            if (btnNewComplaint != null) btnNewComplaint.setVisible(false);
        }
    }

    private void displayCurrentUser() {
        if (session.isLoggedIn()) {
            String userName = userService.getUserNameById(session.getCurrentUserId());
            if (labelCurrentUser != null) labelCurrentUser.setText("👤 " + userName);
            if (labelUserRole != null) {
                labelUserRole.setText(session.isAdmin() ? "🔑 Administrateur" : "👥 Utilisateur");
            }
        }
    }

    private void initStatusFilter() {
        statusFilter.getItems().setAll("Tous", "pending", "resolved", "rejected");
        statusFilter.getSelectionModel().selectFirst();
    }

    @FXML
    private void refreshComplaints() {
        allComplaints.clear();
        List<Complaint> list;

        try {
            if (session.isAdmin()) {
                list = complaintService.getAll();
            } else {
                list = complaintService.getByUserId(session.getCurrentUserId());
            }

            if (list != null && !list.isEmpty()) {
                for (Complaint c : list) {
                    String name = userService.getUserNameById(c.getUserId());
                    c.setUserName(name);
                }
                allComplaints.addAll(list);
            }
            displayComplaintsAsCards(allComplaints);
            updateKpis();
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayComplaintsAsCards(List<Complaint> complaints) {
        complaintsContainer.getChildren().clear();
        if (complaints.isEmpty()) {
            Label emptyLabel = new Label("📭 Aucune réclamation trouvée");
            emptyLabel.getStyleClass().add("empty-label");
            complaintsContainer.getChildren().add(emptyLabel);
            return;
        }
        for (Complaint complaint : complaints)
            complaintsContainer.getChildren().add(createComplaintCard(complaint));
    }

    private VBox createComplaintCard(Complaint complaint) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("#" + complaint.getId());
        idLabel.getStyleClass().add("card-id");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(complaint.getStatus().toUpperCase());
        statusBadge.getStyleClass().addAll("badge", "badge-" + complaint.getStatus().toLowerCase());
        header.getChildren().addAll(idLabel, spacer, statusBadge);

        // User row (admin only)
        HBox userBox = null;
        if (session.isAdmin()) {
            userBox = new HBox(8);
            userBox.setAlignment(Pos.CENTER_LEFT);
            Label userName = new Label(complaint.getUserName());
            userName.getStyleClass().add("card-title");
            userBox.getChildren().addAll(new Label("👤"), userName);
        }

        // Subject
        Label subjectLabel = new Label("📋 " + complaint.getSubject());
        subjectLabel.setWrapText(true);
        subjectLabel.getStyleClass().add("card-title");

        // Date
        HBox dateBox = new HBox(8);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label(complaint.getComplaintDate().format(DATE_FORMAT));
        dateLabel.getStyleClass().add("card-meta");
        dateBox.getChildren().addAll(new Label("📅"), dateLabel);

        // Response box
        VBox responseBox = null;
        if (complaint.getResponse() != null && !complaint.getResponse().isEmpty()) {
            responseBox = new VBox(5);
            responseBox.getStyleClass().add("card-response-box");
            Label responseTitle = new Label("💬 Réponse:");
            responseTitle.getStyleClass().add("card-response-title");
            Label responseText = new Label(complaint.getResponse());
            responseText.setWrapText(true);
            responseText.getStyleClass().add("card-response-text");
            responseBox.getChildren().addAll(responseTitle, responseText);
        }

        // Actions
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        if (session.isAdmin()) {
            Button btnRespond = new Button("📝 Répondre");
            btnRespond.getStyleClass().add("btn-action");
            btnRespond.setOnAction(e -> openRespondModal(complaint));
            actionsBox.getChildren().add(btnRespond);
        } else {
            if ("pending".equalsIgnoreCase(complaint.getStatus())) {
                Button btnEdit = new Button("✏️ Modifier");
                btnEdit.getStyleClass().add("btn-action");
                btnEdit.setOnAction(e -> openEditModal(complaint));

                Button btnDelete = new Button("🗑️ Supprimer");
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setOnAction(e -> deleteComplaint(complaint));
                actionsBox.getChildren().addAll(btnEdit, btnDelete);
            }
        }

        if (userBox != null) card.getChildren().addAll(header, userBox, subjectLabel, dateBox);
        else                  card.getChildren().addAll(header, subjectLabel, dateBox);

        if (responseBox != null) card.getChildren().add(responseBox);
        if (!actionsBox.getChildren().isEmpty()) {
            card.getChildren().add(new Separator());
            card.getChildren().add(actionsBox);
        }
        return card;
    }

    private void updateKpis() {
        int pending = 0, resolved = 0, rejected = 0;
        for (Complaint c : allComplaints) {
            switch (c.getStatus().toLowerCase()) {
                case "pending"  -> pending++;
                case "resolved" -> resolved++;
                case "rejected" -> rejected++;
            }
        }
        if (labelPending  != null) labelPending.setText(String.valueOf(pending));
        if (labelResolved != null) labelResolved.setText(String.valueOf(resolved));
        if (labelRejected != null) labelRejected.setText(String.valueOf(rejected));
    }

    @FXML
    private void applyFilter() {
        String status = statusFilter.getValue();
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();

        List<Complaint> filtered = allComplaints.stream()
                .filter(c -> {
                    boolean statusOk = "Tous".equals(status) || c.getStatus().equalsIgnoreCase(status);
                    boolean searchOk = search.isEmpty() ||
                            (c.getSubject() != null && c.getSubject().toLowerCase().contains(search)) ||
                            (c.getUserName() != null && c.getUserName().toLowerCase().contains(search));
                    return statusOk && searchOk;
                })
                .toList();

        displayComplaintsAsCards(filtered);
    }

    @FXML
    private void openAddComplaintView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddComplaintView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nouvelle Réclamation");
            stage.setScene(new Scene(root, 500, 400));
            stage.showAndWait();
            refreshComplaints();
        } catch (IOException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    private void openEditModal(Complaint complaint) {
        try {
            EditComplaintController.setData(complaint, this::refreshComplaints);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EditComplaintView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Réclamation #" + complaint.getId());
            stage.setScene(new Scene(root, 500, 450));
            stage.showAndWait();
        } catch (IOException e) {
            showError("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    private void openRespondModal(Complaint complaint) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Répondre #" + complaint.getId());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/modals.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/buttons.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("modal-pane");

        // Info strip
        Label lblUser = new Label("👤 " + complaint.getUserName()
                + "   📅 " + complaint.getComplaintDate().format(DATE_FORMAT));
        lblUser.getStyleClass().add("modal-info-label");
        VBox infoStrip = new VBox(lblUser);
        infoStrip.getStyleClass().add("modal-info-strip");

        // Subject preview
        Label lblSubject = new Label(complaint.getSubject());
        lblSubject.setWrapText(true);
        lblSubject.getStyleClass().add("modal-subject-preview");

        // Response
        TextArea responseArea = new TextArea(complaint.getResponse());
        responseArea.setPromptText("Votre réponse...");
        responseArea.setPrefRowCount(5);
        responseArea.setWrapText(true);
        responseArea.getStyleClass().add("modal-textarea");

        // Status
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("pending", "resolved", "rejected");
        statusBox.setValue(complaint.getStatus());
        statusBox.getStyleClass().add("modal-combo");

        Label lReponse = new Label("📝 Réponse :");
        lReponse.getStyleClass().add("modal-field-label");
        Label lStatut = new Label("🏷️ Statut :");
        lStatut.getStyleClass().add("modal-field-label");

        VBox content = new VBox(8,
                infoStrip, lblSubject, new Separator(),
                lReponse, responseArea, lStatut, statusBox);
        content.getStyleClass().add("modal-content");
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    complaint.setResponse(responseArea.getText().trim());
                    complaint.setStatus(statusBox.getValue());
                    complaintService.update(complaint);
                    refreshComplaints();
                    showInfo("✅ Réponse envoyée !");

                    try {
                        String ownerEmail = userService.getUserEmailById(complaint.getUserId());
                        if (ownerEmail != null && !ownerEmail.isBlank()) {
                            emailService.sendComplaintResponse(
                                    ownerEmail,
                                    complaint.getUserName(),
                                    complaint.getSubject(),
                                    complaint.getResponse(),
                                    complaint.getStatus()
                            );
                        }
                    } catch (Exception emailEx) {
                        System.err.println("⚠️ Email non envoyé : " + emailEx.getMessage());
                    }

                } catch (Exception e) {
                    showError("Erreur : " + e.getMessage());
                }
            }
        });
    }

    private void deleteComplaint(Complaint complaint) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer #" + complaint.getId());
        alert.setContentText("Confirmer la suppression ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                complaintService.delete(complaint.getId());
                refreshComplaints();
                showInfo("✅ Réclamation supprimée !");
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            }
        }
    }

    // ── PDF Export ─────────────────────────────────────────────────────────────

    @FXML
    private void exportToPdf() {
        if (allComplaints.isEmpty()) {
            showError("Aucune réclamation à exporter !");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.setInitialFileName("reclamations_" +
                java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        Stage stage = (Stage) btnTransactions.getScene().getWindow();
        java.io.File file = chooser.showSaveDialog(stage);

        if (file != null) {
            try {
                String userName = userService.getUserNameById(session.getCurrentUserId());
                pdfExportService.exportComplaints(
                        allComplaints,
                        userName,
                        session.isAdmin(),
                        file.getAbsolutePath()
                );
                showInfo("✅ PDF exporté avec succès !\n\n📄 " + file.getAbsolutePath());
            } catch (Exception e) {
                showError("Erreur lors de l'export PDF : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    @FXML
    private void navigateToTransactions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TransactionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnTransactions.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 700));
            stage.setTitle("FINTECH - Transactions");
        } catch (IOException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.setHeaderText("Erreur");
        alert.showAndWait();
    }
}