package tn.esprit.controllers;

import tn.esprit.entities.User;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Connection cnx = MyDB.getInstance().getConx();

    // Récupérer le nom d'un user par son ID
    public String getUserNameById(int userId) {
        String sql = "SELECT name FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) {
            System.err.println("Erreur getUserNameById : " + e.getMessage());
        }
        return "Inconnu";
    }

    // Récupérer l'email d'un user par son ID
    public String getUserEmailById(int userId) {
        String sql = "SELECT email FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("email");
        } catch (SQLException e) {
            System.err.println("Erreur getUserEmailById : " + e.getMessage());
        }
        return null;
    }

    // Récupérer tous les users
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getInt("role_id"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null,
                        rs.getBoolean("is_verified"),
                        rs.getString("phone")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getAll users : " + e.getMessage());
        }
        return users;
    }

    // Récupérer un user par ID
    public User getById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getInt("role_id"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null,
                        rs.getBoolean("is_verified"),
                        rs.getString("phone")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur getById user : " + e.getMessage());
        }
        return null;
    }

    // Ajouter un user
    public void add(User user) {
        String sql = "INSERT INTO user (name, email, password_hash, role_id, phone) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setInt(4, user.getRoleId());
            ps.setString(5, user.getPhone());
            ps.executeUpdate();
            System.out.println("User ajouté avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur add user : " + e.getMessage());
        }
    }

    // Mettre à jour un user
    public void update(User user) {
        String sql = "UPDATE user SET name = ?, email = ?, phone = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setInt(4, user.getId());
            ps.executeUpdate();
            System.out.println("User mis à jour avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur update user : " + e.getMessage());
        }
    }

    // Supprimer un user
    public void delete(int id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("User supprimé avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur delete user : " + e.getMessage());
        }
    }
}