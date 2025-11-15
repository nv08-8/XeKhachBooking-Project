package vn.hcmute.busbooking.model;

public class Seat {
    private int id;
    private String seatNumber;
    private String label;
    private String type; // "floor1" or "floor2"
    private boolean isAvailable;
    private boolean isSelected;

    public Seat() {
        this.isSelected = false;
    }

    public Seat(int id, String seatNumber, String type, boolean isAvailable) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.label = seatNumber;
        this.type = type;
        this.isAvailable = isAvailable;
        this.isSelected = false;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getLabel() { return label != null ? label : seatNumber; }
    public void setLabel(String label) { this.label = label; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}