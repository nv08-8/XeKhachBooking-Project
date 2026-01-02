// app/src/main/java/vn/hcmute/busbooking/model/Booking.java
package vn.hcmute.busbooking.model;

public class Booking {
    private int id;
    private int user_id;
    private int trip_id;
    private String seat_label;
    private String status;
    private int price_paid;
    private String created_at;
    private String payment_method;
    private String qr_code;
    private String departure_time;
    private String arrival_time;
    private String operator;
    private String bus_type;
    private String origin;
    private String destination;
    private String distance;
    private String duration;
    private double refund_amount;
    private int refund_percentage;
    private String cancelled_at;
    // Các field mới từ API
    private String trip_cancelled_message;
    private String support_hotline;
    private String pickup_location;
    private String dropoff_location;
    private java.util.List<String> seat_labels;
    private double total_amount;
    private String booking_code;
    private String trip_status;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public int getTrip_id() { return trip_id; }
    public void setTrip_id(int trip_id) { this.trip_id = trip_id; }

    public String getSeat_label() { return seat_label; }
    public void setSeat_label(String seat_label) { this.seat_label = seat_label; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPrice_paid() { return price_paid; }
    public void setPrice_paid(int price_paid) { this.price_paid = price_paid; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getPayment_method() { return payment_method; }
    public void setPayment_method(String payment_method) { this.payment_method = payment_method; }

    public String getQr_code() { return qr_code; }
    public void setQr_code(String qr_code) { this.qr_code = qr_code; }

    public String getDeparture_time() { return departure_time; }
    public void setDeparture_time(String departure_time) { this.departure_time = departure_time; }

    public String getArrival_time() { return arrival_time; }
    public void setArrival_time(String arrival_time) { this.arrival_time = arrival_time; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getBus_type() { return bus_type; }
    public void setBus_type(String bus_type) { this.bus_type = bus_type; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public double getRefund_amount() { return refund_amount; }
    public void setRefund_amount(double refund_amount) { this.refund_amount = refund_amount; }

    public int getRefund_percentage() { return refund_percentage; }
    public void setRefund_percentage(int refund_percentage) { this.refund_percentage = refund_percentage; }

    public String getCancelled_at() { return cancelled_at; }
    public void setCancelled_at(String cancelled_at) { this.cancelled_at = cancelled_at; }

    // Getters và setters cho field mới
    public String getTrip_cancelled_message() { return trip_cancelled_message; }
    public void setTrip_cancelled_message(String trip_cancelled_message) { this.trip_cancelled_message = trip_cancelled_message; }

    public String getSupport_hotline() { return support_hotline; }
    public void setSupport_hotline(String support_hotline) { this.support_hotline = support_hotline; }

    public String getPickup_location() { return pickup_location; }
    public void setPickup_location(String pickup_location) { this.pickup_location = pickup_location; }

    public String getDropoff_location() { return dropoff_location; }
    public void setDropoff_location(String dropoff_location) { this.dropoff_location = dropoff_location; }

    public java.util.List<String> getSeat_labels() { return seat_labels; }
    public void setSeat_labels(java.util.List<String> seat_labels) { this.seat_labels = seat_labels; }

    public double getTotal_amount() { return total_amount; }
    public void setTotal_amount(double total_amount) { this.total_amount = total_amount; }

    public String getBooking_code() { return booking_code; }
    public void setBooking_code(String booking_code) { this.booking_code = booking_code; }

    public String getTrip_status() { return trip_status; }
    public void setTrip_status(String trip_status) { this.trip_status = trip_status; }
}
