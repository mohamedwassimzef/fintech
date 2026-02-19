package tn.esprit;

import tn.esprit.controllers.ComplaintService;
import tn.esprit.controllers.TransactionService;
import tn.esprit.entities.Complaint;
import tn.esprit.entities.Transaction;
import tn.esprit.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        // 1) Connexion DB
        MyDB myDB = MyDB.getInstance();
        Connection cnx = myDB.getConx();

        try {
            if (cnx != null && !cnx.isClosed()) {
                System.out.println("La connexion est OUVERTE ✅");

                // Petit test : SELECT 1
                String sqlTest = "SELECT 1";
                try (Statement stm = cnx.createStatement();
                     ResultSet rs = stm.executeQuery(sqlTest)) {

                    if (rs.next()) {
                        int value = rs.getInt(1);
                        System.out.println("Requête test OK, valeur retournée = " + value);
                    }
                }

                // 2) S'assurer qu'on a un user de test pour respecter la contrainte FK
                int testUserId = ensureTestUser(cnx);
                System.out.println("ID de l'utilisateur de test = " + testUserId);

                // 3) Instancier les services
                TransactionService transactionService = new TransactionService();
                ComplaintService complaintService = new ComplaintService();

                // ==============================
                // TEST TRANSACTION CRUD
                // ==============================
                System.out.println("\n===== TEST TRANSACTION CRUD =====");

                // a) ADD — constructeur (senderId, receiverId, amount, type, status, description, referenceType, referenceId, currency)
                Transaction newTransaction = new Transaction(
                        testUserId,                    // senderId
                        testUserId,                    // receiverId (même user pour test, ou mettre un autre id)
                        new BigDecimal("150.75"),      // montant
                        "credit",                      // type
                        "completed",                   // status
                        "Transaction de test",        // description
                        "online",                      // reference_type
                        null,                          // reference_id
                        "TND"                          // currency
                );
                transactionService.add(newTransaction);

                // b) GET ALL
                List<Transaction> transactions = transactionService.getAll();
                System.out.println("Liste des transactions :");
                for (Transaction t : transactions) {
                    System.out.println(t);
                }

                // ==============================
                // TEST COMPLAINT CRUD
                // ==============================
                System.out.println("\n===== TEST COMPLAINT CRUD =====");

                // a) ADD
                Complaint newComplaint = new Complaint(
                        "Sujet de test",
                        "pending",
                        LocalDate.now(),
                        "Aucune réponse pour l'instant",
                        testUserId
                );
                complaintService.add(newComplaint);

                // b) GET ALL
                List<Complaint> complaints = complaintService.getAll();
                System.out.println("Liste des complaints :");
                for (Complaint c : complaints) {
                    System.out.println(c);
                }

            } else {
                System.out.println("La connexion est FERMÉE ❌");
            }
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        }
    }

    private static int ensureTestUser(Connection cnx) throws SQLException {
        String testEmail = "test.user@fintech.com";

        String checkSql = "SELECT id FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(checkSql)) {
            ps.setString(1, testEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertSql = "INSERT INTO user (name, email, password_hash, role_id, is_verified) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Test User");
            ps.setString(2, testEmail);
            ps.setString(3, "hashed_password");
            ps.setInt(4, 2);
            ps.setInt(5, 1);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new SQLException("Impossible de créer/récupérer l'utilisateur de test.");
    }
}
