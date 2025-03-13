package com.lock.stockit.Models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.lock.stockit.Helpers.SwipeState;

public class ReceiptModel implements Parcelable {
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

    public static final Creator<ReceiptModel> CREATOR = new Creator<>() {
        @Override
        public ReceiptModel createFromParcel(Parcel in) {
            return new ReceiptModel(in);
        }

        @Override
        public ReceiptModel[] newArray(int size) {
            return new ReceiptModel[size];
        }
    };

    protected ReceiptModel(Parcel in) {
        itemName = in.readString();
        itemSize = in.readString();
        itemQty = in.readInt();
        itemUnitPrice = in.readDouble();
        itemTotalPrice = in.readDouble();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(itemName);
        dest.writeString(itemSize);
        dest.writeInt(itemQty);
        dest.writeDouble(itemUnitPrice);
        dest.writeDouble(itemTotalPrice);
    }
}
