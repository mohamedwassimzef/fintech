package tn.esprit.controllers;

import tn.esprit.entities.Complaint;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ComplaintService {

    private final Connection cnx = MyDB.getInstance().getConx();

    // Récupérer toutes les complaints (pour admin)
    public List<Complaint> getAll() {
        List<Complaint> complaints = new ArrayList<>();
        String sql = "SELECT * FROM complaint ORDER BY created_at DESC";

        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Complaint c = new Complaint(
                        rs.getInt("id"),
                        rs.getString("subject"),
                        rs.getString("status"),
                        rs.getDate("complaint_date") != null ?
                                rs.getDate("complaint_date").toLocalDate() : null,
                        rs.getString("response"),
                        rs.getInt("user_id"),
                        rs.getTimestamp("created_at") != null ?
                                rs.getTimestamp("created_at").toLocalDateTime() : null
                );
                complaints.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des complaints : " + e.getMessage());
        }
        return complaints;
    }

    // Récupérer les complaints d'un utilisateur spécifique
    public List<Complaint> getByUserId(int userId) {
        List<Complaint> complaints = new ArrayList<>();
        String sql = "SELECT * FROM complaint WHERE user_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Complaint c = new Complaint(
                        rs.getInt("id"),
                        rs.getString("subject"),
                        rs.getString("status"),
                        rs.getDate("complaint_date") != null ?
                                rs.getDate("complaint_date").toLocalDate() : null,
                        rs.getString("response"),
                        rs.getInt("user_id"),
                        rs.getTimestamp("created_at") != null ?
                                rs.getTimestamp("created_at").toLocalDateTime() : null
                );
                complaints.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des complaints : " + e.getMessage());
        }
        return complaints;
    }

    // Récupérer une complaint par ID
    public Complaint getById(int id) {
        String sql = "SELECT * FROM complaint WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Complaint(
                        rs.getInt("id"),
                        rs.getString("subject"),
                        rs.getString("status"),
                        rs.getDate("complaint_date") != null ?
                                rs.getDate("complaint_date").toLocalDate() : null,
                        rs.getString("response"),
                        rs.getInt("user_id"),
                        rs.getTimestamp("created_at") != null ?
                                rs.getTimestamp("created_at").toLocalDateTime() : null
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la complaint : " + e.getMessage());
        }
        return null;
    }

    // Ajouter une complaint
    public void add(Complaint complaint) {
        String sql = "INSERT INTO complaint (subject, status, complaint_date, user_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, complaint.getSubject());
            ps.setString(2, complaint.getStatus());
            ps.setDate(3, complaint.getComplaintDate() != null ?
                    Date.valueOf(complaint.getComplaintDate()) : Date.valueOf(LocalDate.now()));
            ps.setInt(4, complaint.getUserId());
            ps.executeUpdate();
            System.out.println("Complaint ajoutée avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de la complaint : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Mettre à jour une complaint (avec réponse)
    public void update(Complaint complaint) {
        String sql = "UPDATE complaint SET subject = ?, status = ?, response = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, complaint.getSubject());
            ps.setString(2, complaint.getStatus());
            ps.setString(3, complaint.getResponse());
            ps.setInt(4, complaint.getId());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Complaint mise à jour avec succès !");
            } else {
                System.err.println("Aucune complaint trouvée avec cet ID !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de la complaint : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Supprimer une complaint
    public void delete(int id) {
        String sql = "DELETE FROM complaint WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Complaint supprimée avec succès !");
            } else {
                System.err.println("Aucune complaint trouvée avec cet ID !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la complaint : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Statistiques pour admin
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM complaint WHERE status = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage : " + e.getMessage());
        }
        return 0;
    }
}