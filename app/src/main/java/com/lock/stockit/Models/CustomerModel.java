package com.lock.stockit.Models;

import com.lock.stockit.Helpers.SwipeState;

public class CustomerModel {
    private String name;
    private int transactions;
    private SwipeState state;

    public CustomerModel(String name, int transactions) {
        this.setName(name);
        this.setTransactions(transactions);
        this.setState(SwipeState.NONE);
    }

    public CustomerModel(String name, int transactions, SwipeState state) {
        this.setName(name);
        this.setTransactions(transactions);
        this.setState(state);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTransactions() {
        return transactions;
    }

    public void setTransactions(int transactions) {
        this.transactions = transactions;
    }

    public SwipeState getState() {
        return state;
    }

    public void setState(SwipeState state) {
        this.state = state;
    }
}
