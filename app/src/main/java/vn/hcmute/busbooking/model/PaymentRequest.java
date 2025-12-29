package vn.hcmute.busbooking.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
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
    @SerializedName("items")
    private List<PaymentItem> items;

    public static class PaymentItem {
        @SerializedName("name")
        private String name;
        @SerializedName("quantity")
        private int quantity;
        @SerializedName("price")
        private int price;

        public PaymentItem(String name, int quantity, int price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public int getPrice() { return price; }
        public void setPrice(int price) { this.price = price; }
    }

    public PaymentRequest(String orderId, int amount, List<Integer> booking_ids) {
        this.orderId = orderId;
        this.amount = amount;
        this.booking_ids = booking_ids;
        // Create default items
        this.items = new ArrayList<>();
        this.items.add(new PaymentItem("Vé xe khách", booking_ids.size(), amount / booking_ids.size()));
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
        // Create default items
        this.items = new ArrayList<>();
        this.items.add(new PaymentItem("Vé xe khách", booking_ids.size(), amount / booking_ids.size()));
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

    public List<PaymentItem> getItems() {
        return items;
    }

    public void setItems(List<PaymentItem> items) {
        this.items = items;
    }
}
