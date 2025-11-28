package vn.hcmute.busbooking.model;

public class PromotionValidateResponse {
    private boolean valid;
    private Promotion promotion;
    private double discount;
    private double final_amount;
    private String reason;

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getFinal_amount() { return final_amount; }
    public void setFinal_amount(double final_amount) { this.final_amount = final_amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

