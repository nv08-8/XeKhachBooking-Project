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
    private String payment_time;
    private String departure_time;
    private String arrival_time;
    private String operator;
    private String bus_type;
    private String origin;
    private String destination;
    private String distance;
    private String duration;

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

    public String getPayment_time() { return payment_time; }
    public void setPayment_time(String payment_time) { this.payment_time = payment_time; }

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
}
