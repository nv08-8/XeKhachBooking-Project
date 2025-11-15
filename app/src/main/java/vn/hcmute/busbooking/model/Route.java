// app/src/main/java/vn/hcmute/busbooking/model/Route.java
package vn.hcmute.busbooking.model;

public class Route {
    private int id;
    private String origin;
    private String destination;
    private int distance_km;
    private int duration_min;

    public int getId() { return id; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public int getDistance_km() { return distance_km; }
    public int getDuration_min() { return duration_min; }
}
