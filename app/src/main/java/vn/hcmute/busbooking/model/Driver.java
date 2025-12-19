package vn.hcmute.busbooking.model;

import java.io.Serializable;

public class Driver implements Serializable {
    private int id;
    private String name;
    private String phone;
    private String license_number;

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLicenseNumber() {
        return license_number;
    }

    public void setLicenseNumber(String license_number) {
        this.license_number = license_number;
    }
}
