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

    public Trip() {}

    protected Trip(Parcel in) {
        id = in.readInt();
        int readRouteId = in.readInt();
        routeId = readRouteId == -1 ? null : readRouteId;
        operator = in.readString();
        departureTime = in.readString();
        arrivalTime = in.readString();
        price = in.readDouble();
        int readSeatsTotal = in.readInt();
        seatsTotal = readSeatsTotal == -1 ? null : readSeatsTotal;
        int readSeatsAvailable = in.readInt();
        seatsAvailable = readSeatsAvailable == -1 ? null : readSeatsAvailable;
        status = in.readString();
        origin = in.readString();
        destination = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        if (routeId == null) { dest.writeInt(-1); } else { dest.writeInt(routeId); }
        dest.writeString(operator);
        dest.writeString(departureTime);
        dest.writeString(arrivalTime);
        dest.writeDouble(price);
        if (seatsTotal == null) { dest.writeInt(-1); } else { dest.writeInt(seatsTotal); }
        if (seatsAvailable == null) { dest.writeInt(-1); } else { dest.writeInt(seatsAvailable); }
        dest.writeString(status);
        dest.writeString(origin);
        dest.writeString(destination);
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

    public int getId() { return id; }
    public Integer getRouteId() { return routeId; }
    public String getOperator() { return operator; }
    public String getDepartureTime() { return departureTime; }
    public String getArrivalTime() { return arrivalTime; }
    public double getPrice() { return price; }
    public Integer getSeatsTotal() { return seatsTotal; }
    public Integer getSeatsAvailable() { return seatsAvailable; }
    public String getStatus() { return status; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }

    // Backward-compatible helpers for older UI code
    public String getFromLocation() { return origin; }
    public void setFromLocation(String val) { this.origin = val; }
    public String getToLocation() { return destination; }
    public void setToLocation(String val) { this.destination = val; }
    public String getBusType() { return operator; }
    public void setBusType(String val) { this.operator = val; }
}
