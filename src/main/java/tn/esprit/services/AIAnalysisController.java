package tn.esprit.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.controllers.TransactionService;
import tn.esprit.controllers.UserService;
import tn.esprit.entities.Transaction;
import tn.esprit.utils.GeminiAIService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AIAnalysisController implements Initializable {

    @FXML private Label              labelUser;
    @FXML private Label              labelTxCount;
    @FXML private Label              labelTotalDebit;
    @FXML private Label              labelTotalCredit;
    @FXML private Label              labelBalance;
    @FXML private ComboBox<String>   periodFilter;
    @FXML private VBox               resultContainer;   // ← remplace TextArea
    @FXML private Button             btnAnalyze;
    @FXML private Button             btnTransactions;
    @FXML private VBox               loadingBox;
    @FXML private ProgressIndicator  progressIndicator;
    @FXML private Label              labelLoading;
    @FXML private ScrollPane         resultScroll;

    private final TransactionService transactionService = new TransactionService();
    private final UserService        userService        = new UserService();
    private final GeminiAIService    geminiService      = new GeminiAIService();
    private final SessionManager     session            = SessionManager.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPeriodFilter();
        displayUserInfo();
        loadStats();
        if (loadingBox != null) { loadingBox.setVisible(false); loadingBox.setManaged(false); }
        showPlaceholder();
    }

    private void setupPeriodFilter() {
        if (periodFilter != null) {
            periodFilter.getItems().setAll(
                    "Toutes les transactions", "Les 10 dernières",
                    "Les 20 dernières", "Les 50 dernières"
            );
            periodFilter.setValue("Les 20 dernières");
        }
    }

    private void displayUserInfo() {
        if (session.isLoggedIn() && labelUser != null)
            labelUser.setText("👤 " + userService.getUserNameById(session.getCurrentUserId()));
    }

    private void loadStats() {
        List<Transaction> txList = getTransactions();
        double totalDebit = 0, totalCredit = 0;
        for (Transaction t : txList) {
            if ("debit".equalsIgnoreCase(t.getType())) totalDebit  += t.getAmount().doubleValue();
            else                                        totalCredit += t.getAmount().doubleValue();
        }
        double balance = totalCredit - totalDebit;
        if (labelTxCount   != null) labelTxCount.setText(String.valueOf(txList.size()));
        if (labelTotalDebit  != null) labelTotalDebit.setText(String.format("%.3f TND", totalDebit));
        if (labelTotalCredit != null) labelTotalCredit.setText(String.format("%.3f TND", totalCredit));
        if (labelBalance != null) {
            labelBalance.setText(String.format("%.3f TND", balance));
            labelBalance.setStyle(balance >= 0
                    ? "-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 16px;"
                    : "-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 16px;");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Placeholder initial
    // ══════════════════════════════════════════════════════════════════════════

    private void showPlaceholder() {
        if (resultContainer == null) return;
        resultContainer.getChildren().clear();

        VBox placeholder = new VBox(16);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(60));

        Label icon = new Label("🤖");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Prêt à analyser vos transactions");
        title.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label sub = new Label("Cliquez sur « Analyser avec Groq AI » pour obtenir\nune analyse personnalisée de vos finances.");
        sub.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
        sub.setWrapText(true);
        sub.setAlignment(Pos.CENTER);

        // Mini aperçu des fonctionnalités
        HBox features = new HBox(20);
        features.setAlignment(Pos.CENTER);
        for (String[] f : new String[][]{
                {"📊", "Résumé"},{"🔍", "Analyse"},{"💡", "Conseils"},{"⚠️", "Alertes"}
        }) {
            VBox feat = new VBox(6);
            feat.setAlignment(Pos.CENTER);
            feat.setPadding(new Insets(12, 20, 12, 20));
            feat.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12;");
            Label fi = new Label(f[0]);
            fi.setStyle("-fx-font-size: 22px;");
            Label ft = new Label(f[1]);
            ft.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            feat.getChildren().addAll(fi, ft);
            features.getChildren().add(feat);
        }

        placeholder.getChildren().addAll(icon, title, sub, features);
        resultContainer.getChildren().add(placeholder);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Analyse IA
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void analyzeWithAI() {
        List<Transaction> txList = getTransactions();
        if (txList.isEmpty()) {
            showError("Aucune transaction trouvée.");
            return;
        }

        setLoading(true);
        resultContainer.getChildren().clear();

        String userName = userService.getUserNameById(session.getCurrentUserId());
        String period   = periodFilter != null ? periodFilter.getValue() : "récente";
        double totalDebit = 0, totalCredit = 0;

        List<GeminiAIService.TransactionData> data = new ArrayList<>();
        for (Transaction t : txList) {
            double amount = t.getAmount().doubleValue();
            String receiver = t.getReceiverName() != null ? t.getReceiverName()
                    : userService.getUserNameById(t.getReceiverId());
            data.add(new GeminiAIService.TransactionData(
                    t.getType(), amount,
                    t.getDescription() != null ? t.getDescription() : "—",
                    receiver, t.getStatus()
            ));
            if ("debit".equalsIgnoreCase(t.getType())) totalDebit  += amount;
            else                                         totalCredit += amount;
        }

        final double fd = totalDebit, fc = totalCredit;

        Task<GeminiAIService.AIResult> task = new Task<>() {
            @Override protected GeminiAIService.AIResult call() {
                return geminiService.analyzeTransactions(userName, data, fd, fc, period);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            startCooldown();
            GeminiAIService.AIResult result = task.getValue();
            if (result.isSuccess()) renderAnalysis(result.getAnalysis());
            else                    showErrorCard(result.getErrorMessage());
        });

        task.setOnFailed(e -> {
            setLoading(false);
            startCooldown();
            showErrorCard("Erreur inattendue : " + task.getException().getMessage());
        });

        new Thread(task, "groq-ai-thread").start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Rendu des résultats en cartes colorées
    // ══════════════════════════════════════════════════════════════════════════

    private void renderAnalysis(String rawText) {
        resultContainer.getChildren().clear();

        // Nettoyer le markdown **bold** → texte simple
        String cleaned = rawText
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*",       "$1");

        // Découper par sections (📊 🔍 💡 ⚠️)
        String[] sectionMarkers = {"📊", "🔍", "💡", "⚠️"};
        String[] sectionTitles  = {"Résumé Financier", "Analyse Détaillée",
                "Conseils Personnalisés", "Alertes"};
        String[] sectionColors  = {"#1d4ed8", "#0f766e", "#d97706", "#dc2626"};
        String[] sectionBg      = {"#1e3a6e", "#0d3d38", "#3d2000", "#3d0000"};

        // Trouver les positions des sections dans le texte
        List<int[]> positions = new ArrayList<>();
        for (String marker : sectionMarkers) {
            int idx = cleaned.indexOf(marker);
            positions.add(new int[]{idx});
        }

        // Extraire le contenu de chaque section
        for (int i = 0; i < sectionMarkers.length; i++) {
            int start = positions.get(i)[0];
            if (start < 0) continue;

            // Trouver la fin (début de la prochaine section ou fin du texte)
            int end = cleaned.length();
            for (int j = i + 1; j < sectionMarkers.length; j++) {
                if (positions.get(j)[0] > 0) {
                    end = positions.get(j)[0];
                    break;
                }
            }

            String sectionRaw = cleaned.substring(start, end).trim();
            // Enlever le titre de section (première ligne)
            String content = sectionRaw.contains("\n")
                    ? sectionRaw.substring(sectionRaw.indexOf('\n') + 1).trim()
                    : sectionRaw;

            VBox card = buildSectionCard(
                    sectionMarkers[i] + " " + sectionTitles[i],
                    content,
                    sectionColors[i],
                    sectionBg[i]
            );
            resultContainer.getChildren().add(card);
        }

        // Si aucune section trouvée → afficher le texte brut formaté
        if (resultContainer.getChildren().isEmpty()) {
            renderFallback(cleaned);
        }

        // Scroll vers le haut
        if (resultScroll != null) resultScroll.setVvalue(0);
    }

    private VBox buildSectionCard(String title, String content,
                                  String borderColor, String bgColor) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1.5;"
        );

        // Titre de la section
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 0 0 4 0;"
        );

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + borderColor + "; -fx-opacity: 0.4;");

        // Contenu — chaque ligne devient un item
        VBox contentBox = new VBox(8);
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Lignes numérotées (1. 2. 3.) → style conseil
            if (line.matches("^\\d+\\..*")) {
                HBox item = new HBox(10);
                item.setAlignment(Pos.TOP_LEFT);
                String num = line.substring(0, line.indexOf('.') + 1);
                String text = line.substring(line.indexOf('.') + 1).trim();

                Label numLabel = new Label(num);
                numLabel.setStyle(
                        "-fx-text-fill: " + borderColor.replace("#", "#") + ";" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 13px;" +
                                "-fx-min-width: 24;"
                );
                numLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 13px; -fx-min-width: 24;");

                Label textLabel = new Label(text);
                textLabel.setWrapText(true);
                textLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");
                HBox.setHgrow(textLabel, Priority.ALWAYS);

                item.getChildren().addAll(numLabel, textLabel);
                contentBox.getChildren().add(item);

                // Lignes avec tiret → bullet point
            } else if (line.startsWith("-") || line.startsWith("•")) {
                HBox item = new HBox(10);
                item.setAlignment(Pos.TOP_LEFT);
                String text = line.replaceFirst("^[-•]\\s*", "");

                Label dot = new Label("▸");
                dot.setStyle("-fx-text-fill: " + borderColor + "; -fx-font-size: 12px; -fx-min-width: 16;");

                Label textLabel = new Label(text);
                textLabel.setWrapText(true);
                textLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
                HBox.setHgrow(textLabel, Priority.ALWAYS);

                item.getChildren().addAll(dot, textLabel);
                contentBox.getChildren().add(item);

                // Texte normal
            } else {
                Label textLabel = new Label(line);
                textLabel.setWrapText(true);
                textLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-line-spacing: 2;");
                contentBox.getChildren().add(textLabel);
            }
        }

        card.getChildren().addAll(titleLabel, sep, contentBox);
        return card;
    }

    private void renderFallback(String text) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 14;");
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");
        card.getChildren().add(label);
        resultContainer.getChildren().add(card);
    }

    private void showErrorCard(String msg) {
        resultContainer.getChildren().clear();
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #3d0000; -fx-background-radius: 14; " +
                "-fx-border-color: #dc2626; -fx-border-radius: 14; -fx-border-width: 1.5;");
        Label title = new Label("❌ Erreur");
        title.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label content = new Label(msg);
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 12px;");
        card.getChildren().addAll(title, new Separator(), content);
        resultContainer.getChildren().add(card);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private List<Transaction> getTransactions() {
        List<Transaction> all = session.isAdmin()
                ? transactionService.getAll()
                : transactionService.getByUserId(session.getCurrentUserId());
        if (all == null) return new ArrayList<>();

        // Enrichir avec les noms
        for (Transaction t : all) {
            if (t.getSenderName()   == null) t.setSenderName(userService.getUserNameById(t.getSenderId()));
            if (t.getReceiverName() == null) t.setReceiverName(userService.getUserNameById(t.getReceiverId()));
        }

        String filter = periodFilter != null ? periodFilter.getValue() : "Les 20 dernières";
        return switch (filter) {
            case "Les 10 dernières" -> all.subList(0, Math.min(10, all.size()));
            case "Les 20 dernières" -> all.subList(0, Math.min(20, all.size()));
            case "Les 50 dernières" -> all.subList(0, Math.min(50, all.size()));
            default -> all;
        };
    }

    private void setLoading(boolean on) {
        if (loadingBox != null) { loadingBox.setVisible(on); loadingBox.setManaged(on); }
        if (btnAnalyze != null) {
            btnAnalyze.setDisable(on);
            btnAnalyze.setText(on ? "⏳ Analyse en cours..." : "🤖 Analyser avec Groq AI");
        }
        if (labelLoading != null && on)
            labelLoading.setText("🤖 Groq AI analyse vos transactions...");
    }

    private void startCooldown() {
        if (btnAnalyze == null) return;
        btnAnalyze.setDisable(true);
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        for (int i = 60; i >= 0; i--) {
            final int sec = i;
            timeline.getKeyFrames().add(new javafx.animation.KeyFrame(
                    javafx.util.Duration.seconds(60 - sec),
                    e -> {
                        if (sec > 0) btnAnalyze.setText("⏳ Patienter " + sec + "s...");
                        else { btnAnalyze.setText("🤖 Analyser avec Groq AI"); btnAnalyze.setDisable(false); }
                    }
            ));
        }
        timeline.play();
    }

    @FXML
    private void navigateToTransactions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TransactionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnTransactions.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 700));
            stage.setTitle("FINTECH - Transactions");
        } catch (IOException e) { showError("Erreur : " + e.getMessage()); }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}