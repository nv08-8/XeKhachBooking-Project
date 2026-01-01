package vn.hcmute.busbooking.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Location implements Parcelable {
    private int id;
    private String name;
    private String address;
    private String type;
    private double latitude;
    private double longitude;

    public Location(int id, String name, String address, String type) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.type = type;
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    public Location(int id, String name, String address, String type, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected Location(Parcel in) {
        id = in.readInt();
        name = in.readString();
        address = in.readString();
        type = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    public static final Creator<Location> CREATOR = new Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(address);
        dest.writeString(type);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }
}
