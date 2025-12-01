package vn.hcmute.busbooking.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PaymentRequest {
    @SerializedName("order_id")
    private String orderId;
    @SerializedName("amount")
    private int amount;
    @SerializedName("booking_ids")
    private List<Integer> booking_ids;
    @SerializedName("buyer_name")
    private String buyerName;
    @SerializedName("buyer_email")
    private String buyerEmail;
    @SerializedName("buyer_phone")
    private String buyerPhone;
    @SerializedName("buyer_address")
    private String buyerAddress;

    public PaymentRequest(String orderId, int amount, List<Integer> booking_ids) {
        this.orderId = orderId;
        this.amount = amount;
        this.booking_ids = booking_ids;
    }

    public PaymentRequest(String orderId, int amount, List<Integer> booking_ids,
                          String buyerName, String buyerEmail, String buyerPhone) {
        this.orderId = orderId;
        this.amount = amount;
        this.booking_ids = booking_ids;
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
        this.buyerPhone = buyerPhone;
        this.buyerAddress = "Việt Nam"; // Default
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public List<Integer> getBooking_ids() {
        return booking_ids;
    }

    public void setBooking_ids(List<Integer> booking_ids) {
        this.booking_ids = booking_ids;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public void setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
    }

    public String getBuyerAddress() {
        return buyerAddress;
    }

    public void setBuyerAddress(String buyerAddress) {
        this.buyerAddress = buyerAddress;
    }
}
