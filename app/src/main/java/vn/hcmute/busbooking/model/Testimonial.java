package vn.hcmute.busbooking.model;

public class Testimonial {
    private String customerName;
    private String customerTitle;
    private String testimonialText;
    private int customerImage;
    private int rating;

    // Constructor with 4 parameters (full)
    public Testimonial(String customerName, String customerTitle, String testimonialText, int customerImage) {
        this.customerName = customerName;
        this.customerTitle = customerTitle;
        this.testimonialText = testimonialText;
        this.customerImage = customerImage;
        this.rating = 0;
    }

    // Constructor with 3 parameters (backward compatibility)
    public Testimonial(String customerName, String testimonialText, int customerImage) {
        this.customerName = customerName;
        this.customerTitle = "";
        this.testimonialText = testimonialText;
        this.customerImage = customerImage;
        this.rating = 0;
    }

    // Constructor with rating
    public Testimonial(String customerName, String customerTitle, String testimonialText, int customerImage, int rating) {
        this.customerName = customerName;
        this.customerTitle = customerTitle;
        this.testimonialText = testimonialText;
        this.customerImage = customerImage;
        this.rating = rating;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerTitle() {
        return customerTitle;
    }

    public String getTestimonialText() {
        return testimonialText;
    }

    public int getCustomerImage() {
        return customerImage;
    }

    public int getRating() {
        return rating;
    }
}
