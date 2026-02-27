package tn.esprit.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  GeminiAIService — Analyse de transactions via GROQ API (100% GRATUIT)
 * ══════════════════════════════════════════════════════════════════════════════
 *
 *  POURQUOI GROQ ?
 *  → Gratuit sans limite journalière stricte
 *  → Ultra rapide (< 2 secondes)
 *  → Modèle : llama-3.3-70b-versatile (très puissant)
 *  → 14 400 requêtes/jour gratuites
 *
 *  OBTENIR LA CLÉ API GROQ (2 minutes) :
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │  1. Aller sur https://console.groq.com                          │
 *  │  2. Se connecter (Google ou GitHub)                             │
 *  │  3. API Keys → Create API Key                                   │
 *  │  4. Copier la clé (commence par "gsk_...")                      │
 *  │  5. La coller dans API_KEY ci-dessous                           │
 *  └─────────────────────────────────────────────────────────────────┘
 *
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class GeminiAIService {

    // ── ⚙️  CLÉ API GROQ — https://console.groq.com ──────────────────────────
    private static final String API_KEY = "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";
    // ─────────────────────────────────────────────────────────────────────────

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    // ══════════════════════════════════════════════════════════════════════════
    //  Classes résultat et données
    // ══════════════════════════════════════════════════════════════════════════

    public static class AIResult {
        private final boolean success;
        private final String  analysis;
        private final String  errorMessage;

        private AIResult(boolean success, String analysis, String errorMessage) {
            this.success      = success;
            this.analysis     = analysis;
            this.errorMessage = errorMessage;
        }

        public static AIResult ok(String analysis) { return new AIResult(true,  analysis, null); }
        public static AIResult error(String msg)   { return new AIResult(false, null,     msg);  }

        public boolean isSuccess()       { return success;      }
        public String  getAnalysis()     { return analysis;     }
        public String  getErrorMessage() { return errorMessage; }
    }

    public static class TransactionData {
        public final String type;
        public final double amount;
        public final String description;
        public final String receiverName;
        public final String status;

        public TransactionData(String type, double amount, String description,
                               String receiverName, String status) {
            this.type         = type;
            this.amount       = amount;
            this.description  = description;
            this.receiverName = receiverName;
            this.status       = status;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Analyser les transactions
    // ══════════════════════════════════════════════════════════════════════════

    public AIResult analyzeTransactions(String userName,
                                        List<TransactionData> transactions,
                                        double totalDebit,
                                        double totalCredit,
                                        String period) {
        try {
            if (API_KEY.equals("VOTRE_CLE_GROQ_ICI") || API_KEY.isBlank()) {
                return AIResult.error(
                        "Clé API Groq non configurée.\n\n" +
                                "Étapes (2 minutes, gratuit) :\n" +
                                "1. Allez sur https://console.groq.com\n" +
                                "2. Connectez-vous avec Google ou GitHub\n" +
                                "3. API Keys → Create API Key\n" +
                                "4. Collez la clé (gsk_...) dans GeminiAIService.java → API_KEY"
                );
            }

            // ── Construire la liste des transactions ──────────────────────────
            StringBuilder txList = new StringBuilder();
            for (int i = 0; i < transactions.size(); i++) {
                TransactionData tx = transactions.get(i);
                txList.append(String.format(
                        "%d. [%s] %.3f TND → %s | %s | statut: %s\n",
                        i + 1, tx.type.toUpperCase(), tx.amount,
                        tx.receiverName, tx.description, tx.status
                ));
            }

            double balance = totalCredit - totalDebit;

            String prompt = String.format("""
                Tu es un conseiller financier expert pour une application FINTECH tunisienne.

                Analyse les transactions de %s (%s) et fournis :

                📊 RÉSUMÉ FINANCIER
                [3-4 lignes : bilan général, tendance]

                🔍 ANALYSE DÉTAILLÉE
                [Patterns de dépenses, destinataires fréquents, répartition débit/crédit]

                💡 3 CONSEILS PERSONNALISÉS
                1. [Conseil basé sur les vraies données]
                2. [Conseil basé sur les vraies données]
                3. [Conseil basé sur les vraies données]

                ⚠️ ALERTES
                [Anomalies ou montants élevés — ou "Aucune alerte" si tout est normal]

                DONNÉES :
                • Total DÉBITS  : %.3f TND
                • Total CRÉDITS : %.3f TND
                • SOLDE NET     : %.3f TND (%s)
                • Transactions  : %d

                %s

                Réponds en français. Sois précis et basé sur les vraies données.
                """,
                    userName, period,
                    totalDebit, totalCredit,
                    balance, balance >= 0 ? "✅ positif" : "⚠️ négatif",
                    transactions.size(),
                    txList.toString()
            );

            System.out.println("🤖 Groq → Analyse de " + transactions.size() + " transactions...");

            // ── Format OpenAI-compatible (Groq utilise le même format) ────────
            JSONObject message = new JSONObject();
            message.put("role",    "user");
            message.put("content", prompt);

            JSONArray messages = new JSONArray();
            messages.put(message);

            JSONObject body = new JSONObject();
            body.put("model",       MODEL);
            body.put("messages",    messages);
            body.put("max_tokens",  1500);
            body.put("temperature", 0.7);

            // ── Envoyer la requête ────────────────────────────────────────────
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type",  "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            int    httpCode = response.statusCode();
            String rawBody  = response.body();

            System.out.println("📥 Groq HTTP: " + httpCode);

            if (httpCode == 401) {
                return AIResult.error(
                        "Clé API invalide.\n" +
                                "Vérifiez votre clé sur https://console.groq.com/keys"
                );
            }
            if (httpCode == 429) {
                return AIResult.error(
                        "Quota Groq dépassé.\n" +
                                "Attendez 1 minute et réessayez.\n" +
                                "(14 400 req/jour gratuites — réinitialisé chaque jour)"
                );
            }
            if (httpCode != 200) {
                return AIResult.error("Erreur Groq (HTTP " + httpCode + "):\n" + rawBody);
            }

            // ── Parser la réponse (format OpenAI) ────────────────────────────
            // {
            //   "choices": [{
            //     "message": { "content": "..." }
            //   }]
            // }
            JSONObject json    = new JSONObject(rawBody);
            JSONArray  choices = json.getJSONArray("choices");
            String     text    = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            System.out.println("✅ Analyse Groq reçue (" + text.length() + " chars)");
            return AIResult.ok(text);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AIResult.error("Requête interrompue.");
        } catch (Exception e) {
            System.err.println("❌ Groq error: " + e.getMessage());
            return AIResult.error("Erreur réseau: " + e.getMessage());
        }
    }
}