package com.lock.stockit.Models;

import com.lock.stockit.Helpers.SwipeState;

public class StockModel {
    private String itemName;
    private String itemSize;
    private  double itemQty;
    private String itemQtyType;
    private double itemRegPrice;
    private double itemDscPrice;
    private SwipeState state;

    public StockModel(String itemName, String itemSize, double itemQty, String itemQtyType, double itemRegPrice, double itemDscPrice) {
        this.setItemName(itemName);
        this.setItemSize(itemSize);
        this.setItemQuantity(itemQty);
        this.setItemQtyType(itemQtyType);
        this.setItemRegPrice(itemRegPrice);
        this.setItemDscPrice(itemDscPrice);
        this.setState(SwipeState.NONE);
    }

    public StockModel(String itemName, String itemSize, double itemQty, String itemQtyType, double itemRegPrice, double itemDscPrice, SwipeState state) {
        this.setItemName(itemName);
        this.setItemSize(itemSize);
        this.setItemQuantity(itemQty);
        this.setItemQtyType(itemQtyType);
        this.setItemRegPrice(itemRegPrice);
        this.setItemDscPrice(itemDscPrice);
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

    public double getItemQuantity() {
        return itemQty;
    }

    public void setItemQuantity(double itemQty) {
        this.itemQty = itemQty;
    }

    public String getItemQtyType() {
        return itemQtyType;
    }

    public void setItemQtyType(String itemQtyType) {
        this.itemQtyType = itemQtyType;
    }

    public double getItemRegPrice() {
        return itemRegPrice;
    }

    public void setItemRegPrice(double itemRegPrice) {
        this.itemRegPrice = itemRegPrice;
    }

    public double getItemDscPrice() {
        return itemDscPrice;
    }

    public void setItemDscPrice(double itemDscPrice) {
        this.itemDscPrice = itemDscPrice;
    }

    public SwipeState getState() {
        return state;
    }

    public void setState(SwipeState state) {
        this.state = state;
    }

}
