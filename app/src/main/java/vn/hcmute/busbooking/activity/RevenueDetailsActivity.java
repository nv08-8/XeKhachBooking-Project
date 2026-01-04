package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.BookingDetailsAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class RevenueDetailsActivity extends AppCompatActivity {

    private RecyclerView rvBookingDetails;
    private TextView tvNoDetails;
    private TextView tvDetailsTitle;
    private BookingDetailsAdapter adapter;
    private List<Map<String, Object>> bookingDetailsList = new ArrayList<>();
    private SessionManager sessionManager;
    private boolean isRefund = false; // Biến để track xem có phải hoàn tiền không

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_details);

        sessionManager = new SessionManager(this);

        rvBookingDetails = findViewById(R.id.rvBookingDetails);
        tvNoDetails = findViewById(R.id.tvNoDetails);
        tvDetailsTitle = findViewById(R.id.tvDetailsTitle);

        rvBookingDetails.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingDetailsAdapter(bookingDetailsList);
        rvBookingDetails.setAdapter(adapter);

        String groupBy = getIntent().getStringExtra("groupBy");
        String value = getIntent().getStringExtra("value");
        isRefund = getIntent().getBooleanExtra("isRefund", false); // Lấy mode từ intent

        String titlePrefix = isRefund ? "Chi tiết hoàn tiền cho " : "Chi tiết doanh thu cho ";
        tvDetailsTitle.setText(titlePrefix + value);
        fetchBookingDetails(groupBy, value);
    }

    private void fetchBookingDetails(String groupBy, String value) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        int userId = sessionManager.getUserId();
        String refundType = getIntent().getStringExtra("refundType");
        if (refundType == null) refundType = "";
        // ✅ Lấy payment_method từ intent
        String paymentMethod = getIntent().getStringExtra("paymentMethod");
        if (paymentMethod == null) paymentMethod = "all";

        // Gọi API khác nhau tùy theo mode
        Call<List<Map<String, Object>>> call;
        if (isRefund) {
            // ✅ Truyền payment_method parameter cho hoàn tiền
            call = api.getRevenueRefundDetails(userId, groupBy, value, refundType, paymentMethod);
        } else {
            // ✅ Truyền payment_method parameter
            call = api.getRevenueDetails(userId, groupBy, value, paymentMethod);
        }

        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    bookingDetailsList.clear();
                    bookingDetailsList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    rvBookingDetails.setVisibility(View.VISIBLE);
                    tvNoDetails.setVisibility(View.GONE);
                } else {
                    tvNoDetails.setVisibility(View.VISIBLE);
                    rvBookingDetails.setVisibility(View.GONE);
                    if (!response.isSuccessful()) {
                        tvNoDetails.setText("Không thể tải chi tiết (code=" + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                tvNoDetails.setVisibility(View.VISIBLE);
                rvBookingDetails.setVisibility(View.GONE);
                tvNoDetails.setText("Lỗi: " + t.getMessage());
            }
        });
    }
}
