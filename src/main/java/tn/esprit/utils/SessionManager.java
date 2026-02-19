package tn.esprit.utils;

import tn.esprit.entities.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    public boolean isAdmin() {
        // role_id = 1 pour admin
        return currentUser != null && currentUser.getRoleId() == 1;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        currentUser = null;
    }
}