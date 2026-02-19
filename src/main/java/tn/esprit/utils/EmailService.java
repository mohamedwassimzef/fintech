package tn.esprit.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.internet.MimeUtility;

import java.util.Properties;

/**
 * Sends HTML emails via Gmail SMTP.
 *
 * ⚠️ CONFIGURATION REQUISE :
 *   1. Remplacez SENDER_EMAIL par votre adresse Gmail
 *   2. Remplacez SENDER_PASSWORD par un "App Password" Gmail
 *      (Compte Google → Sécurité → Validation en 2 étapes → Mots de passe des applications)
 *
 * L'envoi est asynchrone (thread séparé) → ne bloque pas l'interface.
 */
public class EmailService {

    // ── ⚙️  CONFIGURATION — MODIFIEZ ICI ─────────────────────────────────────
    private static final String SENDER_EMAIL    = "ftouhmejri11@gmail.com";   // ← votre Gmail
    private static final String SENDER_PASSWORD = "jcnk tsye cqnf exka";     // ← App Password
    private static final String SENDER_NAME     = "FINTECH Platform";
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Notifie le destinataire d'une transaction reçue.
     *
     * @param receiverEmail  email du destinataire
     * @param receiverName   nom du destinataire
     * @param senderName     nom de l'expéditeur
     * @param amount         montant
     * @param currency       devise (TND, USD…)
     * @param description    description de la transaction
     */
    public void sendTransactionNotification(String receiverEmail,
                                            String receiverName,
                                            String senderName,
                                            String amount,
                                            String currency,
                                            String description) {
        String subject = "💰 Vous avez reçu un virement de " + amount + " " + currency;
        String body = buildTransactionEmail(receiverName, senderName, amount, currency, description);
        sendAsync(receiverEmail, subject, body);
    }

    /**
     * Envoie la réponse d'un admin au propriétaire de la réclamation.
     *
     * @param complainantEmail  email de celui qui a soumis la réclamation
     * @param complainantName   son nom
     * @param subject           sujet de la réclamation
     * @param response          réponse de l'admin
     * @param newStatus         nouveau statut (resolved / rejected)
     */
    public void sendComplaintResponse(String complainantEmail,
                                      String complainantName,
                                      String subject,
                                      String response,
                                      String newStatus) {
        String emailSubject = "📋 Réponse à votre réclamation — " + statusLabel(newStatus);
        String body = buildComplaintEmail(complainantName, subject, response, newStatus);
        sendAsync(complainantEmail, emailSubject, body);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Sends in a background thread so the UI never blocks. */
    private void sendAsync(String to, String subject, String htmlBody) {
        new Thread(() -> {
            try {
                send(to, subject, htmlBody);
                System.out.println("✅ Email envoyé à : " + to);
            } catch (Exception e) {
                System.err.println("❌ Erreur envoi email à " + to + " : " + e.getMessage());
            }
        }, "email-sender").start();
    }

    private void send(String to, String subject, String htmlBody)
            throws MessagingException, java.io.IOException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME, "UTF-8"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        try {
            message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
        } catch (java.io.UnsupportedEncodingException e) {
            message.setSubject(subject); // fallback
        }
        message.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(message);
    }

    // ── HTML templates ────────────────────────────────────────────────────────

    private String buildTransactionEmail(String receiverName, String senderName,
                                         String amount, String currency, String description) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#0f172a;font-family:Arial,sans-serif;'>" +
                "<div style='max-width:580px;margin:30px auto;background:#1e293b;border-radius:16px;overflow:hidden;'>" +

                // Header
                "<div style='background:#1e3a8a;padding:28px 32px;'>" +
                "  <span style='color:#facc15;font-size:22px;font-weight:bold;'>FINTECH</span>" +
                "  <span style='color:#93c5fd;font-size:13px;margin-left:10px;'>Finance Platform</span>" +
                "</div>" +

                // Accent bar
                "<div style='height:4px;background:#facc15;'></div>" +

                // Body
                "<div style='padding:32px;'>" +
                "  <p style='color:#94a3b8;font-size:14px;margin:0 0 8px;'>Bonjour,</p>" +
                "  <h2 style='color:white;font-size:20px;margin:0 0 24px;'>💰 Nouveau virement reçu</h2>" +

                // Amount card
                "<div style='background:#064e3b;border-radius:12px;padding:24px;text-align:center;margin-bottom:24px;'>" +
                "  <p style='color:#6ee7b7;font-size:13px;margin:0 0 8px;'>Montant reçu</p>" +
                "  <p style='color:white;font-size:36px;font-weight:bold;margin:0;'>" + amount + " " + currency + "</p>" +
                "</div>" +

                // Details
                "<table style='width:100%;border-collapse:collapse;'>" +
                "  <tr><td style='color:#94a3b8;padding:10px 0;font-size:13px;border-bottom:1px solid #334155;'>De</td>" +
                "      <td style='color:white;padding:10px 0;font-size:13px;font-weight:bold;border-bottom:1px solid #334155;text-align:right;'>" + senderName + "</td></tr>" +
                "  <tr><td style='color:#94a3b8;padding:10px 0;font-size:13px;border-bottom:1px solid #334155;'>Pour</td>" +
                "      <td style='color:white;padding:10px 0;font-size:13px;font-weight:bold;border-bottom:1px solid #334155;text-align:right;'>" + receiverName + "</td></tr>" +
                "  <tr><td style='color:#94a3b8;padding:10px 0;font-size:13px;'>Description</td>" +
                "      <td style='color:#cbd5e1;padding:10px 0;font-size:13px;text-align:right;'>" +
                (description != null && !description.isBlank() ? description : "—") + "</td></tr>" +
                "</table>" +

                "<p style='color:#64748b;font-size:12px;margin-top:28px;'>Ce message a été généré automatiquement par FINTECH. Ne pas répondre à cet email.</p>" +
                "</div>" +

                // Footer
                "<div style='background:#0f172a;padding:16px 32px;text-align:center;'>" +
                "  <span style='color:#475569;font-size:11px;'>© 2026 FINTECH Platform — Tous droits réservés</span>" +
                "</div>" +

                "</div></body></html>";
    }

    private String buildComplaintEmail(String name, String subject,
                                       String response, String status) {
        String statusColor = switch (status.toLowerCase()) {
            case "resolved" -> "#22c55e";
            case "rejected" -> "#ef4444";
            default         -> "#fbbf24";
        };
        String statusIcon = switch (status.toLowerCase()) {
            case "resolved" -> "✅";
            case "rejected" -> "❌";
            default         -> "⏳";
        };

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#0f172a;font-family:Arial,sans-serif;'>" +
                "<div style='max-width:580px;margin:30px auto;background:#1e293b;border-radius:16px;overflow:hidden;'>" +

                "<div style='background:#1e3a8a;padding:28px 32px;'>" +
                "  <span style='color:#facc15;font-size:22px;font-weight:bold;'>FINTECH</span>" +
                "  <span style='color:#93c5fd;font-size:13px;margin-left:10px;'>Finance Platform</span>" +
                "</div>" +
                "<div style='height:4px;background:#facc15;'></div>" +

                "<div style='padding:32px;'>" +
                "  <p style='color:#94a3b8;font-size:14px;margin:0 0 8px;'>Bonjour " + name + ",</p>" +
                "  <h2 style='color:white;font-size:20px;margin:0 0 24px;'>📋 Réponse à votre réclamation</h2>" +

                // Status badge
                "<div style='display:inline-block;background:" + statusColor + ";border-radius:20px;" +
                "  padding:6px 16px;margin-bottom:20px;'>" +
                "  <span style='color:white;font-size:13px;font-weight:bold;'>" + statusIcon + " " + statusLabel(status).toUpperCase() + "</span>" +
                "</div>" +

                // Original subject
                "<div style='background:#0f172a;border-radius:10px;padding:16px;margin-bottom:20px;border-left:4px solid #3b82f6;'>" +
                "  <p style='color:#64748b;font-size:11px;margin:0 0 6px;font-weight:bold;'>VOTRE RÉCLAMATION</p>" +
                "  <p style='color:#cbd5e1;font-size:14px;margin:0;'>" + subject + "</p>" +
                "</div>" +

                // Admin response
                "<div style='background:#0f172a;border-radius:10px;padding:16px;border-left:4px solid " + statusColor + ";'>" +
                "  <p style='color:#64748b;font-size:11px;margin:0 0 6px;font-weight:bold;'>RÉPONSE DE L'ADMINISTRATION</p>" +
                "  <p style='color:white;font-size:14px;margin:0;'>" +
                (response != null && !response.isBlank() ? response : "Votre réclamation a été traitée.") +
                "  </p>" +
                "</div>" +

                "<p style='color:#64748b;font-size:12px;margin-top:28px;'>Ce message a été généré automatiquement par FINTECH. Ne pas répondre à cet email.</p>" +
                "</div>" +

                "<div style='background:#0f172a;padding:16px 32px;text-align:center;'>" +
                "  <span style='color:#475569;font-size:11px;'>© 2026 FINTECH Platform — Tous droits réservés</span>" +
                "</div>" +

                "</div></body></html>";
    }

    private String statusLabel(String status) {
        return switch (status.toLowerCase()) {
            case "resolved" -> "Résolue";
            case "rejected" -> "Rejetée";
            default         -> "En attente";
        };
    }
}