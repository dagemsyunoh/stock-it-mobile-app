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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DeviceModel that = (DeviceModel) obj;
        return address != null && address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }
}
