package vn.hcmute.busbooking.model;

public class PopularRoute {
    private String name;
    private String price;
    private int imageResource;

    public PopularRoute(String name, String price, int imageResource) {
        this.name = name;
        this.price = price;
        this.imageResource = imageResource;
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
}
