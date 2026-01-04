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
    
    @SerializedName("arrival_time")
    private String arrivalTime;

    private String operator;

    @SerializedName("bus_type")
    private String vehicleType;

    private String origin;
    private String destination;
    
    @SerializedName("total_amount")
    private double totalAmount;

    private String price;
    private String duration;
    private String date;

    // ✅ Thêm các fields mới
    @SerializedName("user_name")
    private String userName;

    private String reply;

    @SerializedName("reply_date")
    private String replyDate;

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

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    // ✅ Thêm getters và setters cho các fields mới
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getRouteInfo() {
        if (origin != null && destination != null) {
            return origin + " - " + destination;
        }
        return "";
    }

    public String getTripDate() { return date; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getReplyDate() { return replyDate; }
    public void setReplyDate(String replyDate) { this.replyDate = replyDate; }
}
