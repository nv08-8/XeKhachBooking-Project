package vn.hcmute.busbooking.model;

/**
 * DTO for sending promotion data to API.
 * Excludes id field to prevent duplicate key errors when creating new promotions.
 */
public class PromotionRequest {
    private String code;
    private String discount_type;
    private double discount_value;
    private double min_price;
    private double max_discount;
    private String start_date;
    private String end_date;
    private String status;

    public PromotionRequest() {}

    public PromotionRequest(Promotion promotion) {
        this.code = promotion.getCode();
        this.discount_type = promotion.getDiscount_type();
        this.discount_value = promotion.getDiscount_value();
        this.min_price = promotion.getMin_price();
        this.max_discount = promotion.getMax_discount();
        this.start_date = promotion.getStart_date();
        this.end_date = promotion.getEnd_date();
        this.status = promotion.getStatus();
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDiscount_type() { return discount_type; }
    public void setDiscount_type(String discount_type) { this.discount_type = discount_type; }

    public double getDiscount_value() { return discount_value; }
    public void setDiscount_value(double discount_value) { this.discount_value = discount_value; }

    public double getMin_price() { return min_price; }
    public void setMin_price(double min_price) { this.min_price = min_price; }

    public double getMax_discount() { return max_discount; }
    public void setMax_discount(double max_discount) { this.max_discount = max_discount; }

    public String getStart_date() { return start_date; }
    public void setStart_date(String start_date) { this.start_date = start_date; }

    public String getEnd_date() { return end_date; }
    public void setEnd_date(String end_date) { this.end_date = end_date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

