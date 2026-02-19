package tn.esprit.controllers;

import tn.esprit.entities.Transaction;
import tn.esprit.utils.EmailService;
import tn.esprit.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransactionService {

    private final Connection cnx = MyDB.getInstance().getConx();
    private final EmailService emailService = new EmailService();

    // ── Helper ────────────────────────────────────────────────────────────────
    private Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("sender_id"),
                rs.getInt("receiver_id"),
                rs.getBigDecimal("amount"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getString("description"),
                rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getString("reference_type"),
                (Integer) rs.getObject("reference_id"),
                rs.getString("currency")
        );
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    /** Admin: every row in the table */
    public List<Transaction> getAll() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transaction ORDER BY created_at DESC";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getAll : " + e.getMessage());
        }
        return list;
    }

    /**
     * User view – each transfer produces exactly ONE visible row per user:
     *   • sender   sees the DEBIT  row  (type='debit'  AND sender_id   = userId)
     *   • receiver sees the CREDIT row  (type='credit' AND receiver_id = userId)
     *
     * This prevents the sender from also seeing the mirror credit row,
     * and the receiver from seeing the sender's debit row.
     */
    public List<Transaction> getByUserId(int userId) {
        List<Transaction> list = new ArrayList<>();
        String sql =
                "SELECT * FROM transaction " +
                        "WHERE (type = 'debit'  AND sender_id   = ?) " +
                        "   OR (type = 'credit' AND receiver_id = ?) " +
                        "ORDER BY created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getByUserId : " + e.getMessage());
        }
        return list;
    }

    public Transaction getById(int id) {
        String sql = "SELECT * FROM transaction WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getById : " + e.getMessage());
        }
        return null;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * One call creates TWO rows in the DB:
     *   Row 1 — type='debit',  sender_id=A, receiver_id=B  → A (sender) sees this
     *   Row 2 — type='credit', sender_id=A, receiver_id=B  → B (receiver) sees this
     *
     * sender_id and receiver_id are IDENTICAL in both rows.
     * Only the 'type' column differs.
     */
    public void add(Transaction transaction) {
        String sql =
                "INSERT INTO transaction " +
                        "(sender_id, receiver_id, amount, type, status, description, " +
                        " reference_type, reference_id, currency) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            int        senderId    = transaction.getSenderId();
            int        receiverId  = transaction.getReceiverId();
            BigDecimal amount      = transaction.getAmount();
            String     status      = transaction.getStatus();
            String     description = transaction.getDescription();
            String     refType     = transaction.getReferenceType();
            Integer    refId       = transaction.getReferenceId();
            String     currency    = transaction.getCurrency();

            // ── Row 1: DEBIT — the sender's record ───────────────────────────
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setBigDecimal(3, amount);
            ps.setString(4, "debit");
            ps.setString(5, status);
            ps.setString(6, description);
            ps.setString(7, refType);
            if (refId != null) ps.setInt(8, refId); else ps.setNull(8, Types.INTEGER);
            ps.setString(9, currency);
            ps.executeUpdate();

            // ── Row 2: CREDIT — the receiver's record ────────────────────────
            ps.setInt(1, senderId);      // same sender_id
            ps.setInt(2, receiverId);    // same receiver_id
            ps.setBigDecimal(3, amount);
            ps.setString(4, "credit");   // ← only this changes
            ps.setString(5, status);
            ps.setString(6, description);
            ps.setString(7, refType);
            if (refId != null) ps.setInt(8, refId); else ps.setNull(8, Types.INTEGER);
            ps.setString(9, currency);
            ps.executeUpdate();

            System.out.println("Transaction créée : débit (sender=" + senderId
                    + ") + crédit (receiver=" + receiverId + ")");

            // ── Email notification to receiver (async, non-blocking) ──────────
            try {
                String[] receiver = getUserEmailAndName(receiverId);
                String[] sender   = getUserEmailAndName(senderId);
                if (receiver[0] != null && !receiver[0].isBlank()) {
                    emailService.sendTransactionNotification(
                            receiver[0],
                            receiver[1],
                            sender[1],
                            transaction.getAmount().toPlainString(),
                            transaction.getCurrency(),
                            transaction.getDescription()
                    );
                }
            } catch (Exception e) {
                System.err.println("⚠️ Email non envoyé : " + e.getMessage());
            }

        } catch (SQLException e) {
            System.err.println("Erreur add transaction : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Updates the edited row, then syncs the mirror row automatically.
     * The 'type' column is never touched — debit stays debit, credit stays credit.
     */
    public void update(Transaction transaction) {
        // Update the row that was edited
        String updateSql =
                "UPDATE transaction SET amount = ?, status = ?, description = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(updateSql)) {
            ps.setBigDecimal(1, transaction.getAmount());
            ps.setString(2, transaction.getStatus());
            ps.setString(3, transaction.getDescription());
            ps.setInt(4, transaction.getId());
            ps.executeUpdate();
            System.out.println("Transaction #" + transaction.getId() + " mise à jour.");
        } catch (SQLException e) {
            System.err.println("Erreur update : " + e.getMessage());
            throw new RuntimeException(e);
        }

        // Sync the mirror row (opposite type, same sender/receiver, different id)
        String mirrorType = "debit".equalsIgnoreCase(transaction.getType()) ? "credit" : "debit";
        String syncSql =
                "UPDATE transaction SET amount = ?, status = ?, description = ? " +
                        "WHERE sender_id = ? AND receiver_id = ? AND type = ? AND id != ?";
        try (PreparedStatement ps = cnx.prepareStatement(syncSql)) {
            ps.setBigDecimal(1, transaction.getAmount());
            ps.setString(2, transaction.getStatus());
            ps.setString(3, transaction.getDescription());
            ps.setInt(4, transaction.getSenderId());
            ps.setInt(5, transaction.getReceiverId());
            ps.setString(6, mirrorType);
            ps.setInt(7, transaction.getId());
            int synced = ps.executeUpdate();
            if (synced > 0)
                System.out.println("Miroir (" + mirrorType + ") synchronisé.");
        } catch (SQLException e) {
            // Non-fatal — old single-entry transactions have no mirror
            System.err.println("Pas de miroir à synchroniser : " + e.getMessage());
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Deletes the given row AND its mirror row.
     * Safe for old single-entry transactions — the mirror DELETE simply
     * affects 0 rows without any error.
     */
    public void delete(int id) {
        // Fetch the row first so we know sender/receiver/type for the mirror lookup
        Transaction t = getById(id);

        // Delete the primary row
        String deleteSql = "DELETE FROM transaction WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(deleteSql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Transaction #" + id + " supprimée.");
        } catch (SQLException e) {
            System.err.println("Erreur delete : " + e.getMessage());
            throw new RuntimeException(e);
        }

        // Delete the mirror row if it exists
        if (t != null) {
            String mirrorType = "debit".equalsIgnoreCase(t.getType()) ? "credit" : "debit";
            String mirrorSql =
                    "DELETE FROM transaction " +
                            "WHERE sender_id = ? AND receiver_id = ? AND type = ? AND id != ?";
            try (PreparedStatement ps = cnx.prepareStatement(mirrorSql)) {
                ps.setInt(1, t.getSenderId());
                ps.setInt(2, t.getReceiverId());
                ps.setString(3, mirrorType);
                ps.setInt(4, id);
                int deleted = ps.executeUpdate();
                if (deleted > 0)
                    System.out.println("Miroir (" + mirrorType + ") supprimé aussi.");
            } catch (SQLException e) {
                System.err.println("Erreur suppression miroir : " + e.getMessage());
            }
        }
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    /**
     * Count by status — only counts DEBIT rows to avoid double-counting.
     * (Each transfer = 1 debit + 1 credit; counting both would show double.)
     */
    public Map<String, Integer> countByStatus() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("pending", 0);
        result.put("completed", 0);
        result.put("failed", 0);
        String sql = "SELECT status, COUNT(*) AS cnt FROM transaction WHERE type='debit' GROUP BY status";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) result.put(rs.getString("status"), rs.getInt("cnt"));
        } catch (SQLException e) {
            System.err.println("Erreur countByStatus : " + e.getMessage());
        }
        return result;
    }

    // ── Email helper ──────────────────────────────────────────────────────────

    /** Returns [email, name] for a given user id. Never throws. */
    private String[] getUserEmailAndName(int userId) {
        String sql = "SELECT email, name FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new String[]{rs.getString("email"), rs.getString("name")};
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération user email : " + e.getMessage());
        }
        return new String[]{null, "Utilisateur"};
    }
}