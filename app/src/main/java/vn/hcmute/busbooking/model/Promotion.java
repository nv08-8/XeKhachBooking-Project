package vn.hcmute.busbooking.model;

public class Promotion {
    private long id;
    private String code;
    private String discount_type;
    private double discount_value;
    private double min_price;
    private double max_discount;
    private String start_date;
    private String end_date;
    private String status;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

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

