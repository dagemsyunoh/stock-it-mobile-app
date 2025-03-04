package com.lock.stockit.Model;

import com.lock.stockit.Helper.SwipeState;

public class UserModel {
    private String email;
    private boolean admin;
    private boolean activated;
    private SwipeState state;

    public UserModel(String email, boolean admin, boolean activated) {
        this.setEmail(email);
        this.setAdmin(admin);
        this.setActivated(activated);
        this.setState(SwipeState.NONE);
    }

    public UserModel(String email, boolean admin, boolean activated, SwipeState state) {
        this.setEmail(email);
        this.setAdmin(admin);
        this.setActivated(activated);
        this.setState(state);
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
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

    public SwipeState getState() {
        return state;
    }

    public void setState(SwipeState state) {
        this.state = state;
    }

}
