package vn.hcmute.busbooking.model;

import java.io.Serializable;

public class Promotion implements Serializable {
    private int id;
    private String code;
    private String discount_type;
    private double discount_value;
    private double min_price;
    private double max_discount;
    private String start_date;
    private String end_date;
    private String status;

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDiscountType() {
        return discount_type;
    }

    public void setDiscountType(String discount_type) {
        this.discount_type = discount_type;
    }

    public double getDiscountValue() {
        return discount_value;
    }

    public void setDiscountValue(double discount_value) {
        this.discount_value = discount_value;
    }

    public double getMinPrice() {
        return min_price;
    }

    public void setMinPrice(double min_price) {
        this.min_price = min_price;
    }

    public double getMaxDiscount() {
        return max_discount;
    }

    public void setMaxDiscount(double max_discount) {
        this.max_discount = max_discount;
    }

    public String getStartDate() {
        return start_date;
    }

    public void setStartDate(String start_date) {
        this.start_date = start_date;
    }

    public String getEndDate() {
        return end_date;
    }

    public void setEndDate(String end_date) {
        this.end_date = end_date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
