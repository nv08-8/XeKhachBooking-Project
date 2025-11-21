package vn.hcmute.busbooking.model;

import java.util.List;

public class PaymentRequest {
    private String orderId;
    private int amount;
    private List<Integer> booking_ids;

    public PaymentRequest(String orderId, int amount, List<Integer> booking_ids) {
        this.orderId = orderId;
        this.amount = amount;
        this.booking_ids = booking_ids;
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
}

