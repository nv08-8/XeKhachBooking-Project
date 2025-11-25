package vn.hcmute.busbooking.model;

import com.google.gson.annotations.SerializedName;

public class Seat {
    private int id;

    @SerializedName("label")
    private String label;

    @SerializedName("type")
    private String seatType; // Changed from type to seatType

    @SerializedName("is_booked")
    private boolean isBooked;

    private boolean isSelected;

    public Seat() {
        this.isSelected = false;
    }

    // Added constructor to match usage in SeatSelectionActivity
    public Seat(String label) {
        this.label = label;
        this.isSelected = false;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSeatType() { return seatType; } // Changed from getType
    public void setSeatType(String seatType) { this.seatType = seatType; } // Changed from setType

    public boolean isBooked() { return isBooked; }
    public void setBooked(boolean booked) { isBooked = booked; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
