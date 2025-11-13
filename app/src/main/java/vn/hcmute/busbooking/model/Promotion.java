package vn.hcmute.busbooking.model;

public class Promotion {
    private String title;
    private String description;
    private int imageResource;

    public Promotion(String title, String description, int imageResource) {
        this.title = title;
        this.description = description;
        this.imageResource = imageResource;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getImageResource() {
        return imageResource;
    }
}
