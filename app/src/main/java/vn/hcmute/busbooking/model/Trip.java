package vn.hcmute.busbooking.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Trip implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("route_id")
    private Integer routeId;

    @SerializedName("operator")
    private String operator;

    @SerializedName("departure_time")
    private String departureTime;

    @SerializedName("arrival_time")
    private String arrivalTime;

    @SerializedName("price")
    private double price;

    @SerializedName("seats_total")
    private Integer seatsTotal;

    @SerializedName("seats_available")
    private Integer seatsAvailable;

    @SerializedName("status")
    private String status;

    @SerializedName("origin")
    private String origin;

    @SerializedName("destination")
    private String destination;

    @SerializedName("bus_type")
    private String busType;

    @SerializedName("duration_hours")
    private Double durationHours;

    @SerializedName("distance_km")
    private Integer distanceKm;

    @SerializedName("pickup_point")
    private String pickupPoint;

    @SerializedName("dropoff_point")
    private String dropoffPoint;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("duration_min")
    private Integer durationMin;

    @SerializedName("seat_layout")
    private String seatLayout;

    public Trip() {}

    protected Trip(Parcel in) {
        id = in.readInt();
        routeId = in.readByte() == 0 ? null : in.readInt();
        operator = in.readString();
        departureTime = in.readString();
        arrivalTime = in.readString();
        price = in.readDouble();
        seatsTotal = in.readByte() == 0 ? null : in.readInt();
        seatsAvailable = in.readByte() == 0 ? null : in.readInt();
        status = in.readString();
        origin = in.readString();
        destination = in.readString();
        busType = in.readString();
        durationHours = in.readByte() == 0 ? null : in.readDouble();
        distanceKm = in.readByte() == 0 ? null : in.readInt();
        pickupPoint = in.readString();
        dropoffPoint = in.readString();
        createdAt = in.readString();
        durationMin = in.readByte() == 0 ? null : in.readInt();
        seatLayout = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        if (routeId == null) dest.writeByte((byte) 0); else { dest.writeByte((byte) 1); dest.writeInt(routeId); }
        dest.writeString(operator);
        dest.writeString(departureTime);
        dest.writeString(arrivalTime);
        dest.writeDouble(price);
        if (seatsTotal == null) dest.writeByte((byte) 0); else { dest.writeByte((byte) 1); dest.writeInt(seatsTotal); }
        if (seatsAvailable == null) dest.writeByte((byte) 0); else { dest.writeByte((byte) 1); dest.writeInt(seatsAvailable); }
        dest.writeString(status);
        dest.writeString(origin);
        dest.writeString(destination);
        dest.writeString(busType);
        if (durationHours == null) dest.writeByte((byte) 0); else { dest.writeByte((byte) 1); dest.writeDouble(durationHours); }
        if (distanceKm == null) dest.writeByte((byte) 0); else { dest.writeByte((byte) 1); dest.writeInt(distanceKm); }
        dest.writeString(pickupPoint);
        dest.writeString(dropoffPoint);
        dest.writeString(createdAt);
        if (durationMin == null) dest.writeByte((byte) 0); else { dest.writeByte((byte) 1); dest.writeInt(durationMin); }
        dest.writeString(seatLayout);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Trip> CREATOR = new Creator<Trip>() {
        @Override
        public Trip createFromParcel(Parcel in) {
            return new Trip(in);
        }

        @Override
        public Trip[] newArray(int size) {
            return new Trip[size];
        }
    };
    
    // Getters and Setters
    public String getSeatLayout() { return seatLayout; }
    public void setSeatLayout(String seatLayout) { this.seatLayout = seatLayout; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public Integer getSeatsTotal() { return seatsTotal; }
    public void setSeatsTotal(Integer seatsTotal) { this.seatsTotal = seatsTotal; }
    public Integer getSeatsAvailable() { return seatsAvailable; }
    public void setSeatsAvailable(Integer seatsAvailable) { this.seatsAvailable = seatsAvailable; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getBusType() { return busType; }
    public void setBusType(String busType) { this.busType = busType; }
    public Double getDurationHours() { return durationHours; }
    public void setDurationHours(Double durationHours) { this.durationHours = durationHours; }
    public Integer getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Integer distanceKm) { this.distanceKm = distanceKm; }
    public String getPickupPoint() { return pickupPoint; }
    public void setPickupPoint(String pickupPoint) { this.pickupPoint = pickupPoint; }
    public String getDropoffPoint() { return dropoffPoint; }
    public void setDropoffPoint(String dropoffPoint) { this.dropoffPoint = dropoffPoint; }
    public String getFromLocation() { return origin; }
    public String getToLocation() { return destination; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }
}
