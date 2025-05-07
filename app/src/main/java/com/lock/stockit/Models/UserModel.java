package com.lock.stockit.Models;

import com.lock.stockit.Helpers.SwipeState;

public class UserModel {
    private String name;
    private String email;
    private boolean admin;
    private boolean activated;
    private String store;
    private SwipeState state;

    public UserModel(String name, String email, boolean admin, boolean activated, String store) {
        this.setName(name);
        this.setEmail(email);
        this.setAdmin(admin);
        this.setActivated(activated);
        this.setStore(store);
        this.setState(SwipeState.NONE);
    }

    public UserModel(String name, String email, boolean admin, boolean activated, String store, SwipeState state) {
        this.setName(name);
        this.setEmail(email);
        this.setAdmin(admin);
        this.setActivated(activated);
        this.setStore(store);
        this.setState(state);
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

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public SwipeState getState() {
        return state;
    }

    public void setState(SwipeState state) {
        this.state = state;
    }

}
