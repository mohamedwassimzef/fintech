package tn.esprit.utils;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  PaymeeService — Intégration Paymee.tn (Sandbox)
 * ══════════════════════════════════════════════════════════════════════════════
 *
 *  FLUX SANDBOX (pas de webhook) :
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │ 1. createPayment() → reçoit { token, payment_url }             │
 *  │ 2. Navigateur ouvre payment_url                                 │
 *  │ 3. Après paiement → URL navigateur contient &transaction=XXXXX │
 *  │ 4. L'utilisateur saisit ce transaction_id dans l'app           │
 *  │ 5. confirmByTransactionId(txId) → vérifie via API              │
 *  └─────────────────────────────────────────────────────────────────┘
 *
 *  NOTE : Paymee sandbox n'a pas d'endpoint GET /payments/{token}
 *         La vraie vérification en production se fait via webhook.
 *
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class PaymeeService {

    // ── ⚙️  CREDENTIALS SANDBOX ───────────────────────────────────────────────
    private static final String API_TOKEN  = "68562c72da6f425ca8549a6807a535fe5c0d89e0";
    private static final String BASE_URL   = "https://sandbox.paymee.tn";
    private static final String CREATE_URL = BASE_URL + "/api/v2/payments/create";
    // ─────────────────────────────────────────────────────────────────────────

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // ══════════════════════════════════════════════════════════════════════════
    //  Classes résultat
    // ══════════════════════════════════════════════════════════════════════════

    public static class PaymentResult {
        private final boolean success;
        private final String  token;
        private final String  paymentUrl;
        private final String  errorMessage;

        private PaymentResult(boolean success, String token,
                              String paymentUrl, String errorMessage) {
            this.success      = success;
            this.token        = token;
            this.paymentUrl   = paymentUrl;
            this.errorMessage = errorMessage;
        }

        public static PaymentResult ok(String token, String paymentUrl) {
            return new PaymentResult(true, token, paymentUrl, null);
        }

        public static PaymentResult error(String msg) {
            return new PaymentResult(false, null, null, msg);
        }

        public boolean isSuccess()       { return success;      }
        public String  getToken()        { return token;        }
        public String  getPaymentUrl()   { return paymentUrl;   }
        public String  getErrorMessage() { return errorMessage; }
    }

    public static class PaymentStatus {
        private final boolean paid;
        private final double  amount;
        private final int     transactionId;
        private final String  errorMessage;

        private PaymentStatus(boolean paid, double amount,
                              int transactionId, String errorMessage) {
            this.paid          = paid;
            this.amount        = amount;
            this.transactionId = transactionId;
            this.errorMessage  = errorMessage;
        }

        public static PaymentStatus paid(double amount, int transactionId) {
            return new PaymentStatus(true, amount, transactionId, null);
        }

        public static PaymentStatus notPaid() {
            return new PaymentStatus(false, 0, 0, null);
        }

        public static PaymentStatus error(String msg) {
            return new PaymentStatus(false, 0, 0, msg);
        }

        public boolean isPaid()          { return paid;          }
        public double  getAmount()       { return amount;        }
        public int     getTransactionId(){ return transactionId; }
        public String  getErrorMessage() { return errorMessage;  }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ÉTAPE 1 — Créer un paiement
    // ══════════════════════════════════════════════════════════════════════════

    public PaymentResult createPayment(double amount, String note,
                                       String firstName, String lastName,
                                       String email, String phone,
                                       String orderId) {
        try {
            JSONObject body = new JSONObject();
            body.put("amount",      amount);
            body.put("note",        note);
            body.put("first_name",  firstName);
            body.put("last_name",   lastName != null ? lastName : ".");
            body.put("email",       email != null ? email : "user@fintech.tn");
            body.put("phone",       phone != null ? phone : "+21600000000");
            body.put("webhook_url", "https://example.com/webhook");
            body.put("return_url",  "https://example.com/return");
            body.put("cancel_url",  "https://example.com/cancel");
            body.put("order_id",    orderId);

            System.out.println("📤 Paymee → createPayment: " + amount + " DT, order=" + orderId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CREATE_URL))
                    .header("Content-Type",  "application/json")
                    .header("Authorization", "Token " + API_TOKEN)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📥 Create HTTP: " + response.statusCode());
            System.out.println("📥 Create body: " + response.body());

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return PaymentResult.error("Token API invalide.");
            }
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                return PaymentResult.error("Erreur Paymee (code " + response.statusCode() + ").");
            }

            // Nettoyer BOM/espaces avant {
            String raw = response.body();
            int idx = raw.indexOf('{');
            if (idx > 0) raw = raw.substring(idx);

            JSONObject json = new JSONObject(raw.trim());

            if (!json.optBoolean("status", false)) {
                return PaymentResult.error("Paymee: " + json.optString("message", "Erreur inconnue"));
            }

            JSONObject data       = json.getJSONObject("data");
            String     token      = data.getString("token");
            String     paymentUrl = data.getString("payment_url");

            System.out.println("✅ Paiement créé ! Token: " + token);
            System.out.println("🔗 URL: " + paymentUrl);

            return PaymentResult.ok(token, paymentUrl);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentResult.error("Requête interrompue.");
        } catch (Exception e) {
            System.err.println("❌ createPayment error: " + e.getMessage());
            return PaymentResult.error("Erreur réseau: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ÉTAPE 3 — Vérifier le paiement par polling
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Tente de vérifier le paiement en essayant plusieurs endpoints Paymee.
     * Les endpoints testés dans l'ordre :
     *   GET /api/v2/payments/{token}/check
     *   GET /api/v1/payments/{token}/check
     *   GET /api/v2/payments/{token}
     */
    public PaymentStatus checkPayment(String token) {
        String[] endpoints = {
                BASE_URL + "/api/v2/payments/" + token + "/check",
                BASE_URL + "/api/v1/payments/" + token + "/check",
                BASE_URL + "/api/v2/payments/" + token,
                BASE_URL + "/api/v1/payments/" + token,
        };

        for (String url : endpoints) {
            System.out.println("🔍 Essai endpoint: " + url);
            PaymentStatus result = tryCheckEndpoint(url);

            if (result.getErrorMessage() != null
                    && result.getErrorMessage().startsWith("SKIP")) {
                // Cet endpoint n'existe pas → essayer le suivant
                continue;
            }
            return result; // succès ou vraie erreur
        }

        return PaymentStatus.error(
                "Aucun endpoint de vérification disponible en sandbox.\n" +
                        "Paymee sandbox ne supporte pas le polling.\n" +
                        "Entrez manuellement le transaction_id visible dans l'URL."
        );
    }

    private PaymentStatus tryCheckEndpoint(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Token " + API_TOKEN)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            int    code = response.statusCode();
            String body = response.body();

            System.out.println("   HTTP " + code + " | body: " +
                    (body != null ? body.substring(0, Math.min(body.length(), 120)) : "null"));

            // 404 ou 405 → endpoint inexistant → SKIP
            if (code == 404 || code == 405 || code == 301 || code == 302) {
                return PaymentStatus.error("SKIP");
            }

            if (code == 401 || code == 403) {
                return PaymentStatus.error("Token API invalide (HTTP " + code + ")");
            }

            if (body == null || body.trim().isEmpty()) {
                return PaymentStatus.error("SKIP");
            }

            // Trouver le premier '{' (ignore BOM, espaces, HTML avant JSON)
            int braceIdx = body.indexOf('{');
            if (braceIdx < 0) {
                // Pas de JSON → probablement HTML d'une page de login → SKIP
                System.out.println("   → Pas de JSON (HTML reçu) → SKIP");
                return PaymentStatus.error("SKIP");
            }

            String json = body.substring(braceIdx).trim();

            // Vérifier que c'est du JSON valide
            if (!json.startsWith("{")) {
                return PaymentStatus.error("SKIP");
            }

            org.json.JSONObject obj  = new org.json.JSONObject(json);
            org.json.JSONObject data = obj.optJSONObject("data");

            if (data == null) {
                // Peut-être que "data" contient payment_status directement
                boolean direct = obj.optBoolean("payment_status", false);
                if (direct) return PaymentStatus.paid(obj.optDouble("amount", 0), obj.optInt("transaction_id", 0));
                return PaymentStatus.error("SKIP");
            }

            boolean paid = data.optBoolean("payment_status", false);
            if (paid) {
                double amt = data.optDouble("amount", 0);
                int    tid = data.optInt("transaction_id", 0);
                System.out.println("✅ Paiement confirmé via polling ! txId=" + tid);
                return PaymentStatus.paid(amt, tid);
            }

            return PaymentStatus.notPaid();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentStatus.error("Interrompu");
        } catch (Exception e) {
            System.out.println("   → Exception: " + e.getMessage() + " → SKIP");
            return PaymentStatus.error("SKIP");
        }
    }

    /**
     * Confirme le paiement via le transaction_id visible dans l'URL navigateur.
     * Utilisé uniquement si le polling ne fonctionne pas en sandbox.
     */
    public PaymentStatus confirmByTransactionId(int transactionId, double originalAmount) {
        if (transactionId <= 0) {
            return PaymentStatus.error("Transaction ID invalide.");
        }
        System.out.println("✅ Confirmation manuelle sandbox → txId=" + transactionId);
        return PaymentStatus.paid(originalAmount, transactionId);
    }
}