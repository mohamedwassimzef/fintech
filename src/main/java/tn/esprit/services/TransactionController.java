package tn.esprit.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.controllers.TransactionService;
import tn.esprit.controllers.UserService;
import tn.esprit.entities.Transaction;
import tn.esprit.utils.PdfExportService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TransactionController implements Initializable {

    @FXML private VBox transactionsContainer;
    @FXML private Label labelTotalDebit;
    @FXML private Label labelTotalCredit;
    @FXML private Label labelCount;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelUserRole;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Button btnComplaints;
    @FXML private VBox statsPanel;       // container injected from FXML

    private final TransactionService transactionService = new TransactionService();
    private final UserService userService = new UserService();
    private final ObservableList<Transaction> allTransactions = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final SessionManager session = SessionManager.getInstance();

    private final PdfExportService pdfExportService = new PdfExportService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!session.isLoggedIn()) {
            showError("Aucun utilisateur connecté !");
            return;
        }
        initFilters();
        displayCurrentUser();
        refreshTable();
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────

    private void displayCurrentUser() {
        String userName = userService.getUserNameById(session.getCurrentUserId());
        if (labelCurrentUser != null) labelCurrentUser.setText("👤 " + userName);
        if (labelUserRole != null)
            labelUserRole.setText(session.isAdmin() ? "🔑 Administrateur" : "👥 Utilisateur");
    }

    private void initFilters() {
        statusFilter.getItems().setAll("Tous", "completed", "pending", "failed");
        statusFilter.getSelectionModel().selectFirst();
        typeFilter.getItems().setAll("Tous", "debit", "credit");
        typeFilter.getSelectionModel().selectFirst();
    }

    // ── Data ───────────────────────────────────────────────────────────────────

    @FXML
    private void refreshTable() {
        allTransactions.clear();

        // Admin sees every row; regular user sees rows where they are sender OR receiver
        List<Transaction> list = session.isAdmin()
                ? transactionService.getAll()
                : transactionService.getByUserId(session.getCurrentUserId());

        if (list != null) {
            for (Transaction t : list) {
                t.setSenderName(userService.getUserNameById(t.getSenderId()));
                t.setReceiverName(userService.getUserNameById(t.getReceiverId()));
            }
            allTransactions.addAll(list);
        }

        displayTransactionsAsCards(allTransactions);
        updateKpis();
        buildStatsChart();
    }

    // ── Card rendering ─────────────────────────────────────────────────────────

    private void displayTransactionsAsCards(List<Transaction> transactions) {
        transactionsContainer.getChildren().clear();
        if (transactions.isEmpty()) {
            Label empty = new Label("💸 Aucune transaction trouvée");
            empty.getStyleClass().add("empty-label");
            transactionsContainer.getChildren().add(empty);
            return;
        }
        for (Transaction t : transactions)
            transactionsContainer.getChildren().add(createTransactionCard(t));
    }

    private VBox createTransactionCard(Transaction transaction) {
        boolean isDebit = "debit".equalsIgnoreCase(transaction.getType());

        VBox card = new VBox(12);
        card.getStyleClass().add(isDebit ? "card-debit" : "card-credit");

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("#" + transaction.getId());
        idLabel.getStyleClass().add("card-id");

        Label typeIcon = new Label(isDebit ? "💳 DÉBIT" : "💰 CRÉDIT");
        typeIcon.getStyleClass().addAll("badge", isDebit ? "badge-debit" : "badge-credit");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(transaction.getStatus().toUpperCase());
        statusBadge.getStyleClass().addAll("badge", "badge-" + transaction.getStatus().toLowerCase());
        header.getChildren().addAll(idLabel, typeIcon, spacer, statusBadge);

        // Sender / receiver
        HBox senderBox = new HBox(8);
        senderBox.setAlignment(Pos.CENTER_LEFT);
        Label senderLbl = new Label("De: " + transaction.getSenderName());
        senderLbl.getStyleClass().add("card-subtitle");
        senderBox.getChildren().addAll(new Label("📤"), senderLbl);

        HBox receiverBox = new HBox(8);
        receiverBox.setAlignment(Pos.CENTER_LEFT);
        Label receiverLbl = new Label("Vers: " + transaction.getReceiverName());
        receiverLbl.getStyleClass().add("card-title");
        receiverBox.getChildren().addAll(new Label("📥"), receiverLbl);

        VBox usersBox = new VBox(8, senderBox, receiverBox);

        // Description
        Label desc = new Label("📝 " + (transaction.getDescription() != null
                ? transaction.getDescription() : "Aucune description"));
        desc.setWrapText(true);
        desc.getStyleClass().add("card-subtitle");

        // Amount + date
        Label amountLbl = new Label(transaction.getAmount() + " " + transaction.getCurrency());
        amountLbl.getStyleClass().add("card-amount");
        Label amountMeta = new Label("💵 Montant");
        amountMeta.getStyleClass().add("card-label-small");

        Label dateLbl = new Label(transaction.getCreatedAt() != null
                ? transaction.getCreatedAt().format(DATE_FORMAT) : "N/A");
        dateLbl.getStyleClass().add("card-meta");
        Label dateMeta = new Label("📅 Date");
        dateMeta.getStyleClass().add("card-label-small");

        HBox infoBox = new HBox(20,
                new VBox(3, amountMeta, amountLbl),
                new VBox(3, dateMeta, dateLbl));
        infoBox.setAlignment(Pos.CENTER_LEFT);

        // Actions
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        boolean canManage = session.isAdmin()
                || (isDebit && transaction.getSenderId() == session.getCurrentUserId());
        if (canManage) {
            Button btnEdit = new Button("✏️ Modifier");
            btnEdit.getStyleClass().add("btn-action");
            btnEdit.setOnAction(e -> openEditModal(transaction));

            Button btnDelete = new Button("🗑️ Supprimer");
            btnDelete.getStyleClass().add("btn-danger");
            btnDelete.setOnAction(e -> deleteTransaction(transaction));
            actionsBox.getChildren().addAll(btnEdit, btnDelete);
        }

        card.getChildren().addAll(header, usersBox, desc, infoBox);
        if (!actionsBox.getChildren().isEmpty()) {
            card.getChildren().add(new Separator());
            card.getChildren().add(actionsBox);
        }
        return card;
    }

    // ── KPIs ───────────────────────────────────────────────────────────────────

    private void updateKpis() {
        BigDecimal totalDebit  = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Transaction t : allTransactions) {
            if ("debit".equalsIgnoreCase(t.getType()))        totalDebit  = totalDebit.add(t.getAmount());
            else if ("credit".equalsIgnoreCase(t.getType())) totalCredit = totalCredit.add(t.getAmount());
        }
        if (labelTotalDebit  != null) labelTotalDebit.setText(totalDebit + " TND");
        if (labelTotalCredit != null) labelTotalCredit.setText(totalCredit + " TND");
        if (labelCount       != null) labelCount.setText(String.valueOf(allTransactions.size()));
    }

    // ── Statistics chart ───────────────────────────────────────────────────────

    /**
     * Draws a simple bar chart directly on a Canvas.
     * Bars represent counts per status (pending / completed / failed)
     * and a type breakdown (debit / credit).
     */
    private void buildStatsChart() {
        if (statsPanel == null) return;
        statsPanel.getChildren().clear();

        if (allTransactions.isEmpty()) return;

        // Compute directly from the already-filtered list — never query the whole DB
        int pending = 0, completed = 0, failed = 0, debit = 0, credit = 0;
        for (Transaction t : allTransactions) {
            switch (t.getStatus().toLowerCase()) {
                case "pending"   -> pending++;
                case "completed" -> completed++;
                case "failed"    -> failed++;
            }
            if ("debit".equalsIgnoreCase(t.getType()))        debit++;
            else if ("credit".equalsIgnoreCase(t.getType())) credit++;
        }

        HBox charts = new HBox(12);
        charts.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        charts.getChildren().addAll(
                buildBarChart(
                        "Par statut",
                        new String[]{"pending", "completed", "failed"},
                        new int[]{pending, completed, failed},
                        new Color[]{Color.web("#fbbf24"), Color.web("#22c55e"), Color.web("#ef4444")},
                        260, 110
                ),
                buildBarChart(
                        "Par type",
                        new String[]{"debit", "credit"},
                        new int[]{debit, credit},
                        new Color[]{Color.web("#ef4444"), Color.web("#22c55e")},
                        160, 110
                )
        );

        Label title = new Label("📊 Statistiques");
        title.getStyleClass().add("sidebar-role");

        HBox wrapper = new HBox(16);
        wrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        wrapper.getStyleClass().add("filter-bar");
        wrapper.getChildren().addAll(title, charts);

        statsPanel.getChildren().add(wrapper);
    }

    private VBox buildBarChart(String subtitle, String[] labels, int[] values,
                               Color[] colors, double canvasW, double canvasH) {
        Canvas canvas = new Canvas(canvasW, canvasH);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Transparent background
        gc.clearRect(0, 0, canvasW, canvasH);

        int    n       = labels.length;
        double barW    = canvasW / (n * 2.5);
        double spacing = (canvasW - n * barW) / (n + 1);
        double maxVal  = 0;
        for (int v : values) if (v > maxVal) maxVal = v;
        if (maxVal == 0) maxVal = 1;
        double maxBarH  = canvasH - 36;
        int    totalAll = 0;
        for (int v : values) totalAll += v;

        for (int i = 0; i < n; i++) {
            double x    = spacing + i * (barW + spacing);
            double barH = (values[i] / maxVal) * maxBarH;
            double y    = canvasH - 22 - barH;

            // Subtle bar background track
            gc.setFill(Color.web("#334155"));
            gc.fillRoundRect(x, canvasH - 22 - maxBarH, barW, maxBarH, 4, 4);

            // Actual bar
            gc.setFill(colors[i]);
            gc.fillRoundRect(x, y, barW, barH, 4, 4);

            // Value above
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            String valStr = String.valueOf(values[i]);
            gc.fillText(valStr, x + barW / 2 - valStr.length() * 3.5, y - 3);

            // Label below
            gc.setFill(Color.web("#64748b"));
            gc.setFont(Font.font("System", 10));
            gc.fillText(labels[i], x + barW / 2 - labels[i].length() * 2.8, canvasH - 5);
        }

        Label sub = new Label(subtitle);
        sub.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");
        VBox box = new VBox(4, sub, canvas);
        return box;
    }

    // ── Filter ─────────────────────────────────────────────────────────────────

    @FXML
    private void applyFilter() {
        String status = statusFilter.getValue();
        String type   = typeFilter.getValue();
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();

        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> {
                    boolean statusOk = "Tous".equals(status) || t.getStatus().equalsIgnoreCase(status);
                    boolean typeOk   = "Tous".equals(type)   || t.getType().equalsIgnoreCase(type);
                    boolean searchOk = search.isEmpty()
                            || (t.getDescription() != null && t.getDescription().toLowerCase().contains(search))
                            || (t.getReceiverName() != null && t.getReceiverName().toLowerCase().contains(search))
                            || (t.getSenderName()   != null && t.getSenderName().toLowerCase().contains(search));
                    return statusOk && typeOk && searchOk;
                })
                .toList();
        displayTransactionsAsCards(filtered);
    }

    // ── Add transaction modal ──────────────────────────────────────────────────

    @FXML
    private void openAddTransactionView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddTransactionView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nouvelle Transaction");
            stage.setScene(new Scene(root, 550, 500));
            stage.showAndWait();
            refreshTable();
        } catch (IOException e) {
            showError("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    // ── Edit modal ─────────────────────────────────────────────────────────────

    private void openEditModal(Transaction transaction) {
        try {
            EditTransactionController.setData(transaction, this::refreshTable);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EditTransactionView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Transaction #" + transaction.getId());
            stage.setScene(new Scene(root, 500, 430));
            stage.showAndWait();
        } catch (IOException e) {
            showError("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    private void deleteTransaction(Transaction transaction) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la transaction #" + transaction.getId() + " ?");
        alert.setContentText("Montant: " + transaction.getAmount() + " " + transaction.getCurrency());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                transactionService.delete(transaction.getId());
                refreshTable();
                showInfo("✅ Transaction supprimée !");
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            }
        }
    }

    // ── PDF Export ─────────────────────────────────────────────────────────────

    @FXML
    private void exportToPdf() {
        if (allTransactions.isEmpty()) {
            showError("Aucune transaction à exporter !");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.setInitialFileName("transactions_" +
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy")) + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        Stage stage = (Stage) btnComplaints.getScene().getWindow();
        java.io.File file = chooser.showSaveDialog(stage);

        if (file != null) {
            try {
                String userName = userService.getUserNameById(session.getCurrentUserId());
                pdfExportService.exportTransactions(
                        allTransactions,
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
    private void navigateToComplaints() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ComplaintView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnComplaints.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 700));
            stage.setTitle("FINTECH - Réclamations");
        } catch (IOException e) {
            showError("Erreur navigation : " + e.getMessage());
        }
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText("Erreur");
        a.showAndWait();
    }
}