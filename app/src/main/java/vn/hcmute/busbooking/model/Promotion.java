package vn.hcmute.busbooking.model;

import java.io.Serializable;
import java.util.Map;

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

    // UI fields
    private String title;
    private String description;
    private int imageResId;

    public Promotion() {}

    // Constructor for Mock Data
    public Promotion(String title, String description, int imageResId) {
        this.title = title;
        this.description = description;
        this.imageResId = imageResId;
    }

    // Constructor from API Map
    public Promotion(Map<String, Object> map) {
        if (map == null) return;

        this.id = getInt(map, "id");
        this.code = getString(map, "code");
        this.discount_type = getString(map, "discount_type");
        this.discount_value = getDouble(map, "discount_value");
        this.min_price = getDouble(map, "min_price");
        this.max_discount = getDouble(map, "max_discount");
        this.start_date = getString(map, "start_date");
        this.end_date = getString(map, "end_date");
        this.status = getString(map, "status");

        // Generate title and description for UI
        this.title = this.code;
        if ("percent".equals(this.discount_type)) {
            this.description = "Giảm " + (int)this.discount_value + "%";
        } else {
            this.description = String.format("Giảm %,.0fđ", this.discount_value);
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }

    // Getters and Setters (with snake_case for backward compatibility)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDiscount_type() { return discount_type; } // Keep snake_case
    public void setDiscount_type(String discount_type) { this.discount_type = discount_type; }
    public double getDiscount_value() { return discount_value; } // Keep snake_case
    public void setDiscount_value(double discount_value) { this.discount_value = discount_value; }
    public double getMin_price() { return min_price; } // Keep snake_case
    public void setMin_price(double min_price) { this.min_price = min_price; }
    public double getMax_discount() { return max_discount; } // Keep snake_case
    public void setMax_discount(double max_discount) { this.max_discount = max_discount; }
    public String getStart_date() { return start_date; } // Keep snake_case
    public void setStart_date(String start_date) { this.start_date = start_date; }
    public String getEnd_date() { return end_date; } // Keep snake_case
    public void setEnd_date(String end_date) { this.end_date = end_date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
}
