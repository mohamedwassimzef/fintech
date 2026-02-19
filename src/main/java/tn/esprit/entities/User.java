package tn.esprit.entities;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private int roleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isVerified;
    private String phone;

    // Constructeur vide
    public User() {
    }

    // Constructeur complet
    public User(int id, String name, String email, String passwordHash, int roleId,
                LocalDateTime createdAt, LocalDateTime updatedAt, boolean isVerified, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isVerified = isVerified;
        this.phone = phone;
    }

    // Constructeur sans ID (pour l'insertion)
    public User(String name, String email, String passwordHash, int roleId, String phone) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
        this.phone = phone;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", roleId=" + roleId +
                ", isVerified=" + isVerified +
                ", phone='" + phone + '\'' +
                '}';
    }
}