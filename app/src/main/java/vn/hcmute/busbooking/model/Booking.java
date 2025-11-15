package vn.hcmute.busbooking.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

public class Booking implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("origin")
    private String origin;

    @SerializedName("destination")
    private String destination;

    @SerializedName("departure_time")
    private String departureTime;

    @SerializedName("seat_label")
    private String seatLabel;

    @SerializedName("price_paid")
    private double pricePaid;

    @SerializedName("status")
    private String status;

    protected Booking(Parcel in) {
        id = in.readInt();
        origin = in.readString();
        destination = in.readString();
        departureTime = in.readString();
        seatLabel = in.readString();
        pricePaid = in.readDouble();
        status = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(origin);
        dest.writeString(destination);
        dest.writeString(departureTime);
        dest.writeString(seatLabel);
        dest.writeDouble(pricePaid);
        dest.writeString(status);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Booking> CREATOR = new Creator<Booking>() {
        @Override
        public Booking createFromParcel(Parcel in) {
            return new Booking(in);
        }

        @Override
        public Booking[] newArray(int size) {
            return new Booking[size];
        }
    };

    // Getters
    public int getId() { return id; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getDepartureTime() { return departureTime; }
    public String getSeatLabel() { return seatLabel; }
    public double getPricePaid() { return pricePaid; }
    public String getStatus() { return status; }
}
