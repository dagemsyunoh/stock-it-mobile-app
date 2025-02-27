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
}
