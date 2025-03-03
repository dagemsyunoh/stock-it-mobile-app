package com.lock.stockit.Model;

public class UserData {
    String email;
    boolean admin;
    boolean activated;

    public UserData(String email, boolean admin, boolean activated) {
        this.email = email;
        this.admin = admin;
        this.activated = activated;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isActivated() {
        return activated;
    }
}
