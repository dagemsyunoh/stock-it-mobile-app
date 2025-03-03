package com.lock.stockit;

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

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }
}
