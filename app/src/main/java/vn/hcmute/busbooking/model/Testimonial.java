package vn.hcmute.busbooking.model;

public class Testimonial {
    private String customerName;
    private String testimonialText;
    private int customerImage;

    public Testimonial(String customerName, String testimonialText, int customerImage) {
        this.customerName = customerName;
        this.testimonialText = testimonialText;
        this.customerImage = customerImage;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getTestimonialText() {
        return testimonialText;
    }

    public int getCustomerImage() {
        return customerImage;
    }
}
