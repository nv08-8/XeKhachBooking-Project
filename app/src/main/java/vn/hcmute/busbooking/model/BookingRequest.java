package vn.hcmute.busbooking.model;

public class BookingRequest {
    private int user_id;
    private int trip_id;
    private String seat_label;

    public BookingRequest(int user_id, int trip_id, String seat_label) {
        this.user_id = user_id;
        this.trip_id = trip_id;
        this.seat_label = seat_label;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getTrip_id() {
        return trip_id;
    }

    public void setTrip_id(int trip_id) {
        this.trip_id = trip_id;
    }

    public String getSeat_label() {
        return seat_label;
    }

    public void setSeat_label(String seat_label) {
        this.seat_label = seat_label;
    }
}

