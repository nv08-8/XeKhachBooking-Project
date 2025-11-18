package vn.hcmute.busbooking.model;

import com.google.gson.annotations.SerializedName;

public class Booking {

    @SerializedName("id")
    private int id;

    @SerializedName("trip_id")
    private int tripId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("seat_number")
    private String seatNumber;

    @SerializedName("total_price")
    private int totalPrice;

    @SerializedName("status")
    private String status;

    // Thông tin của chuyến đi lồng vào
    @SerializedName("from_location")
    private String fromLocation;

    @SerializedName("to_location")
    private String toLocation;

    @SerializedName("departure_time")
    private String departureTime;

    // Getters
    public int getId() { return id; }
    public int getTripId() { return tripId; }
    public int getUserId() { return userId; }
    public String getSeatNumber() { return seatNumber; }
    public int getTotalPrice() { return totalPrice; }
    public String getStatus() { return status; }
    public String getFromLocation() { return fromLocation; }
    public String getToLocation() { return toLocation; }
    public String getDepartureTime() { return departureTime; }
}
