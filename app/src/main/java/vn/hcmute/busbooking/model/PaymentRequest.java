package vn.hcmute.busbooking.model;

public class PaymentRequest {
    private String payment_method;

    public PaymentRequest(String payment_method) {
        this.payment_method = payment_method;
    }

    public String getPayment_method() {
        return payment_method;
    }

    public void setPayment_method(String payment_method) {
        this.payment_method = payment_method;
    }
}

