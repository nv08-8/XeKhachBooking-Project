package vn.hcmute.busbooking.model;

public class Payment {
    private int id;
    private String method; // "FUTAPay", "ZaloPay", "MoMo", "Visa"
    private double amount;
    private String status; // "pending", "completed", "failed"
    private String transactionId;
    private long timestamp;

    public Payment(int id, String method, double amount, String status, String transactionId, long timestamp) {
        this.id = id;
        this.method = method;
        this.amount = amount;
        this.status = status;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}