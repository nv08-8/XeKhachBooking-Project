package vn.hcmute.busbooking.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Trip implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("from_location")
    private String fromLocation;

    @SerializedName("to_location")
    private String toLocation;

    @SerializedName("departure_time")
    private String departureTime;

    @SerializedName("arrival_time")
    private String arrivalTime;

    @SerializedName("price")
    private int price;

    @SerializedName("bus_type")
    private String busType;

    public Trip(int id, String fromLocation, String toLocation, String departureTime, String arrivalTime, int price, String busType) {
        this.id = id;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.busType = busType;
    }

    protected Trip(Parcel in) {
        id = in.readInt();
        fromLocation = in.readString();
        toLocation = in.readString();
        departureTime = in.readString();
        arrivalTime = in.readString();
        price = in.readInt();
        busType = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(fromLocation);
        dest.writeString(toLocation);
        dest.writeString(departureTime);
        dest.writeString(arrivalTime);
        dest.writeInt(price);
        dest.writeString(busType);
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getBusType() {
        return busType;
    }

    public void setBusType(String busType) {
        this.busType = busType;
    }
}
