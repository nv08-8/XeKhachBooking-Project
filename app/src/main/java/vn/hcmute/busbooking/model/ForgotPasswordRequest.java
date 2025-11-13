package vn.hcmute.busbooking.model;

public class ForgotPasswordRequest {
    private String email;

    public ForgotPasswordRequest(String email) {
        this.email = email;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
