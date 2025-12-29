package vn.hcmute.busbooking.model;

import com.google.gson.annotations.SerializedName;

public class Seat {
    private int id;

    @SerializedName("label")
    private String label;

    @SerializedName(value = "seatType", alternate = {"type"})
    private String seatType;

    @SerializedName(value = "isBooked", alternate = {"is_booked"})
    private boolean isBooked;

    @SerializedName("booking_id")
    private Long bookingId;

    private boolean isSelected;

    public Seat() {
        this.isSelected = false;
    }

    public Seat(String label) {
        this.label = label;
        this.isSelected = false;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }

    public boolean isBooked() { return isBooked; }
    public void setBooked(boolean booked) { isBooked = booked; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    // Kiểm tra xem ghế có phải được admin đánh dấu (no booking_id)
    public boolean isAdminMarked() {
        return isBooked && bookingId == null;
    }
}
