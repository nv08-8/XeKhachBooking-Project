package vn.hcmute.busbooking.model;

import com.google.gson.annotations.SerializedName;

public class Feedback {
    private int id;
    
    @SerializedName("user_id")
    private int userId;
    
    @SerializedName("booking_id")
    private int bookingId;
    
    private int rating;
    private String comment;
    
    @SerializedName("created_at")
    private String createdAt;
    
    // Additional fields for display
    @SerializedName("feedback_date")
    private String feedbackDate;
    
    @SerializedName("departure_time")
    private String departureTime;
    
    private String operator;
    private String origin;
    private String destination;
    
    @SerializedName("total_amount")
    private double totalAmount;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getFeedbackDate() { return feedbackDate; }
    public void setFeedbackDate(String feedbackDate) { this.feedbackDate = feedbackDate; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
}
