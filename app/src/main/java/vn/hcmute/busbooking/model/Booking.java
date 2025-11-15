// app/src/main/java/vn/hcmute/busbooking/model/Booking.java
package vn.hcmute.busbooking.model;

public class Booking {
    private int id;
    private String seat_label;
    private String status;
    private int price_paid;
    private String created_at;
    private String departure_time;
    private String arrival_time;
    private String operator;
    private String origin;
    private String destination;

    public int getId() { return id; }
    public String getSeat_label() { return seat_label; }
    public String getStatus() { return status; }
    public int getPrice_paid() { return price_paid; }
    public String getCreated_at() { return created_at; }
    public String getDeparture_time() { return departure_time; }
    public String getArrival_time() { return arrival_time; }
    public String getOperator() { return operator; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
}
