package com.lock.stockit.Models;

public class DeviceModel {
    String name;
    String address;

    public DeviceModel(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
