package vn.hcmute.busbooking.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

public class Location implements Parcelable {
    private int id;
    private String name;
    private String address;
    private String type;
    
    @SerializedName("estimated_time")
    private String estimatedTime;

    public Location(int id, String name, String address, String type, String estimatedTime) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.type = type;
        this.estimatedTime = estimatedTime;
    }

    protected Location(Parcel in) {
        id = in.readInt();
        name = in.readString();
        address = in.readString();
        type = in.readString();
        estimatedTime = in.readString();
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

    public String getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
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
        dest.writeString(estimatedTime);
    }
}
