package vn.hcmute.busbooking.model;

import com.google.gson.annotations.SerializedName;

public class PopularRoute {
    private String name;
    private String price;
    private int imageResource;

    @SerializedName(value = "image_url", alternate = {"image"})
    private String imageUrl;

    // Constructor with drawable resource (for backward compatibility)
    public PopularRoute(String name, String price, int imageResource) {
        this.name = name;
        this.price = price;
        this.imageResource = imageResource;
        this.imageUrl = null;
    }

    // Constructor with image URL (for server-loaded images)
    public PopularRoute(String name, String price, String imageUrl) {
        this.name = name;
        this.price = price;
        this.imageResource = 0;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public int getImageResource() {
        return imageResource;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImage() {
        return imageUrl;
    }

    public boolean hasImageUrl() {
        return imageUrl != null && !imageUrl.isEmpty();
    }
}
