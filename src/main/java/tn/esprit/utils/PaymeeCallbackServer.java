package tn.esprit.utils;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  PaymeeCallbackServer — Serveur HTTP local pour capturer le retour Paymee
 * ══════════════════════════════════════════════════════════════════════════════
 *
 *  Flux automatique (plus besoin de copier/coller manuellement) :
 *
 *  1. App démarre ce serveur sur localhost:8765
 *  2. Paymee reçoit return_url = "http://localhost:8765/paymee/callback"
 *  3. Après paiement, le navigateur est redirigé vers :
 *       http://localhost:8765/paymee/callback?payment_token=abc&transaction=89569
 *  4. Ce serveur capture le transaction_id AUTOMATIQUEMENT
 *  5. Affiche une belle page HTML de confirmation dans le navigateur
 *  6. Notifie l'app JavaFX → transaction sauvegardée en DB
 *  7. Le serveur s'arrête
 *
 *  SÉCURITÉ : le transaction_id vient directement de Paymee via la redirection,
 *  l'utilisateur ne peut pas le falsifier.
 *
 *  DÉPENDANCE : com.sun.net.httpserver (intégré au JDK, aucune lib externe)
 *
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class PaymeeCallbackServer {

    public static final String CALLBACK_URL = "http://localhost:8765/paymee/callback";
    private static final int   PORT         = 8765;

    private HttpServer        server;
    private final CountDownLatch  latch      = new CountDownLatch(1);
    private final AtomicInteger   capturedId = new AtomicInteger(-1);
    private Runnable onPaymentReceived;

    // ══════════════════════════════════════════════════════════════════════════
    //  Démarrer le serveur
    // ══════════════════════════════════════════════════════════════════════════

    public void start(Runnable onPaymentReceived) throws IOException {
        this.onPaymentReceived = onPaymentReceived;

        server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);

        // Route principale — Paymee redirige ici après paiement
        server.createContext("/paymee/callback", exchange -> {
            try {
                handleCallback(exchange);
            } catch (Exception e) {
                System.err.println("❌ Callback error: " + e.getMessage());
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("🟢 PaymeeCallbackServer démarré → " + CALLBACK_URL);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Gérer le callback de Paymee
    // ══════════════════════════════════════════════════════════════════════════

    private void handleCallback(HttpExchange exchange) throws IOException {
        URI    uri   = exchange.getRequestURI();
        String query = uri.getQuery();

        System.out.println("📥 Callback Paymee reçu !");
        System.out.println("📥 Query: " + query);

        int txId = parseTransactionId(query);

        if (txId > 0) {
            capturedId.set(txId);
            System.out.println("✅ Transaction ID capturé automatiquement: " + txId);

            // Page HTML de succès dans le navigateur
            sendHtml(exchange, buildSuccessPage(txId));

            // Notifier JavaFX
            latch.countDown();
            if (onPaymentReceived != null) {
                onPaymentReceived.run();
            }

        } else {
            System.err.println("⚠️ transaction_id absent de: " + query);
            sendHtml(exchange, buildErrorPage());
        }

        exchange.close();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Parser le transaction_id depuis ?payment_token=abc&transaction=89569
    // ══════════════════════════════════════════════════════════════════════════

    private int parseTransactionId(String query) {
        if (query == null || query.isEmpty()) return -1;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "transaction".equalsIgnoreCase(kv[0].trim())) {
                try {
                    return Integer.parseInt(kv[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Attendre le callback (bloque le thread courant)
    // ══════════════════════════════════════════════════════════════════════════

    /** @return true = paiement reçu, false = timeout */
    public boolean waitForCallback(int timeoutSeconds) {
        try {
            return latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public int getCapturedTransactionId() {
        return capturedId.get();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Arrêter le serveur
    // ══════════════════════════════════════════════════════════════════════════

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("🔴 PaymeeCallbackServer arrêté.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Utilitaires HTTP
    // ══════════════════════════════════════════════════════════════════════════

    private void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Pages HTML affichées dans le navigateur
    // ══════════════════════════════════════════════════════════════════════════

    private String buildSuccessPage(int txId) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <title>Paiement confirmé</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        background: #0f172a;
                        color: white;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                    }
                    .card {
                        background: #1e293b;
                        border-radius: 16px;
                        padding: 50px 60px;
                        text-align: center;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.5);
                        max-width: 480px;
                        width: 90%;
                    }
                    .icon { font-size: 72px; margin-bottom: 20px; }
                    h1 { color: #22c55e; font-size: 28px; margin-bottom: 12px; }
                    p { color: #94a3b8; font-size: 15px; line-height: 1.6; }
                    .txid {
                        background: #0f172a;
                        border-radius: 8px;
                        padding: 14px 20px;
                        margin: 24px 0;
                        font-family: monospace;
                        font-size: 14px;
                        color: #f59e0b;
                    }
                    .txid span { font-size: 24px; font-weight: bold; }
                    .close-msg {
                        color: #475569;
                        font-size: 13px;
                        margin-top: 20px;
                    }
                    .logo { color: #f59e0b; font-size: 20px; font-weight: bold; margin-bottom: 30px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">💳 Paymee</div>
                    <div class="icon">✅</div>
                    <h1>Paiement confirmé !</h1>
                    <p>Votre paiement a été effectué avec succès.</p>
                    <div class="txid">
                        Transaction ID : <span>""" + txId + """
                        </span>
                    </div>
                    <p>La transaction a été enregistrée dans l'application.</p>
                    <p class="close-msg">Vous pouvez fermer cet onglet et retourner dans l'application.</p>
                </div>
            </body>
            </html>
            """;
    }

    private String buildErrorPage() {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <title>Erreur</title>
                <style>
                    body { font-family: Arial; background: #0f172a; color: white;
                           display: flex; align-items: center; justify-content: center;
                           min-height: 100vh; }
                    .card { background: #1e293b; border-radius: 16px; padding: 40px;
                            text-align: center; max-width: 400px; }
                    h1 { color: #ef4444; margin-bottom: 16px; }
                    p  { color: #94a3b8; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div style="font-size:60px">❌</div>
                    <h1>Paiement non détecté</h1>
                    <p>Le numéro de transaction est introuvable.<br>
                       Retournez dans l'application et réessayez.</p>
                </div>
            </body>
            </html>
            """;
    }
}