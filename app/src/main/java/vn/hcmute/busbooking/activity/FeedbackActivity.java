package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedbackActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText etComment;
    private Button btnSubmit;
    private ApiService apiService;
    private int bookingId = -1;
    private int tripId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etComment);
        btnSubmit = findViewById(R.id.btnSubmitReview);

        apiService = ApiClient.getClient().create(ApiService.class);

        bookingId = getIntent().getIntExtra("booking_id", -1);
        tripId = getIntent().getIntExtra("trip_id", -1);

        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        if (bookingId <= 0 || tripId <= 0) {
            Toast.makeText(this, "Không xác định được vé hoặc chuyến", Toast.LENGTH_SHORT).show();
            return;
        }
        int rating = (int) ratingBar.getRating();
        String comment = etComment.getText().toString().trim();

        Map<String, Object> body = new HashMap<>();
        body.put("booking_id", bookingId);
        body.put("trip_id", tripId);
        body.put("rating", rating);
        body.put("comment", comment);

        btnSubmit.setEnabled(false);
        apiService.submitReview(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(FeedbackActivity.this, "Cảm ơn bạn đã gửi nhận xét", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(FeedbackActivity.this, "Gửi nhận xét thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Toast.makeText(FeedbackActivity.this, "Lỗi khi gửi nhận xét: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
