package com.lock.stockit.Models;

import com.lock.stockit.Helpers.SwipeState;

public class StockModel {
    private String itemName;
    private String itemSize;
    private  int itemQty;
    private double itemPrice;
    private SwipeState state;

    public StockModel(String itemName, String itemSize, int itemQty, double itemPrice) {
        this.setItemName(itemName);
        this.setItemSize(itemSize);
        this.setItemQuantity(itemQty);
        this.setItemPrice(itemPrice);
        this.setState(SwipeState.NONE);
    }

    public StockModel(String itemName, String itemSize, int itemQty, double itemPrice, SwipeState state) {
        this.setItemName(itemName);
        this.setItemSize(itemSize);
        this.setItemQuantity(itemQty);
        this.setItemPrice(itemPrice);
        this.setState(state);
    }
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemSize() {
        return itemSize;
    }

    public void setItemSize(String itemSize) {
        this.itemSize = itemSize;
    }

    public int getItemQuantity() {
        return itemQty;
    }

    public void setItemQuantity(int itemQty) {
        this.itemQty = itemQty;
    }

    public double getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(double itemPrice) {
        this.itemPrice = itemPrice;
    }

    public SwipeState getState() {
        return state;
    }

    public void setState(SwipeState state) {
        this.state = state;
    }

}
