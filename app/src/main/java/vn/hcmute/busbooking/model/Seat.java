package vn.hcmute.busbooking.model;

import com.google.gson.annotations.SerializedName;

public class Seat {
    private int id;

    // Map json "label" từ backend vào biến này
    @SerializedName("label")
    private String label;

    // Map json "type" (nếu có)
    @SerializedName("type")
    private String type;

    // QUAN TRỌNG: Map "is_booked" từ backend vào biến isBooked
    @SerializedName("is_booked")
    private boolean isBooked;

    // Biến nội bộ, không liên quan API
    private boolean isSelected;

    public Seat() {
        this.isSelected = false;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    // Để tương thích với code cũ nếu bạn lỡ dùng getSeatNumber ở đâu đó
    public String getSeatNumber() { return label; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // Hàm này trả về true nếu ghế ĐÃ ĐƯỢC ĐẶT
    public boolean isBooked() { return isBooked; }
    public void setBooked(boolean booked) { isBooked = booked; }

    // Hàm tiện ích: Ghế khả dụng khi CHƯA bị đặt
    public boolean isAvailable() { return !isBooked; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
