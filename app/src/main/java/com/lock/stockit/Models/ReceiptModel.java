package com.lock.stockit.Models;

import com.lock.stockit.Helpers.SwipeState;

public class ReceiptModel {
    private String itemName;
    private String itemSize;
    private  int itemQty;
    private double itemUnitPrice;
    private double itemTotalPrice;
    private SwipeState state;

    public ReceiptModel(String itemName, String itemSize, int itemQty, double itemUnitPrice, double itemTotalPrice) {
        this.setItemName(itemName);
        this.setItemSize(itemSize);
        this.setItemQuantity(itemQty);
        this.setItemUnitPrice(itemUnitPrice);
        this.setItemTotalPrice(itemTotalPrice);
        this.setState(SwipeState.NONE);
    }

    public ReceiptModel(String itemName, String itemSize, int itemQty, double itemUnitPrice, double itemTotalPrice, SwipeState state) {
        this.setItemName(itemName);
        this.setItemSize(itemSize);
        this.setItemQuantity(itemQty);
        this.setItemUnitPrice(itemUnitPrice);
        this.setItemTotalPrice(itemTotalPrice);
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

    public double getItemUnitPrice() {
        return itemUnitPrice;
    }

    public void setItemUnitPrice(double itemUnitPrice) {
        this.itemUnitPrice = itemUnitPrice;
    }
    public double getItemTotalPrice() {
        return itemTotalPrice;
    }

    public void setItemTotalPrice(double itemTotalPrice) {
        this.itemTotalPrice = itemTotalPrice;
    }

    public SwipeState getState() {
        return state;
    }

    public void setState(SwipeState state) {
        this.state = state;
    }

}
