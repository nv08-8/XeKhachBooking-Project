package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private Spinner spinnerTrip;
    private LinearLayout tripFilterContainer;
    private BookingDetailsAdapter adapter;
    private List<Map<String, Object>> bookingDetailsList = new ArrayList<>();
    private List<Map<String, Object>> allBookingDetails = new ArrayList<>(); // Lưu tất cả dữ liệu gốc
    private SessionManager sessionManager;
    private boolean isRefund = false; // Biến để track xem có phải hoàn tiền không

    private String currentGroupBy;
    private String currentValue;
    private String selectedTripId = null; // Biến để track trip được chọn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_details);

        sessionManager = new SessionManager(this);

        // Thiết lập toolbar và xử lý nút mũi tên
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvBookingDetails = findViewById(R.id.rvBookingDetails);
        tvNoDetails = findViewById(R.id.tvNoDetails);
        tvDetailsTitle = findViewById(R.id.tvDetailsTitle);
        spinnerTrip = findViewById(R.id.spinnerTrip);
        tripFilterContainer = findViewById(R.id.tripFilterContainer);

        rvBookingDetails.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingDetailsAdapter(bookingDetailsList);
        rvBookingDetails.setAdapter(adapter);

        String groupBy = getIntent().getStringExtra("groupBy");
        String value = getIntent().getStringExtra("value");
        isRefund = getIntent().getBooleanExtra("isRefund", false); // Lấy mode từ intent

        currentGroupBy = groupBy;
        currentValue = value;

        String titlePrefix = isRefund ? "Chi tiết hoàn tiền cho " : "Chi tiết doanh thu cho ";
        tvDetailsTitle.setText(titlePrefix + value);

        // Setup spinner listener
        setupTripSpinnerListener();

        fetchBookingDetails(groupBy, value);
    }

    private void setupTripSpinnerListener() {
        spinnerTrip.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedTripId = null; // "Tất cả chuyến"
                } else {
                    selectedTripId = (String) parent.getItemAtPosition(position);
                }
                filterBookingDetails();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTripId = null;
                filterBookingDetails();
            }
        });
    }

    private void filterBookingDetails() {
        if (selectedTripId == null) {
            // Hiển thị tất cả
            bookingDetailsList.clear();
            bookingDetailsList.addAll(allBookingDetails);
        } else {
            // Lọc theo chuyến được chọn
            bookingDetailsList.clear();
            for (Map<String, Object> booking : allBookingDetails) {
                Object tripId = booking.get("trip_id");
                if (tripId != null && tripId.toString().equals(selectedTripId)) {
                    bookingDetailsList.add(booking);
                }
            }
        }

        if (bookingDetailsList.isEmpty()) {
            tvNoDetails.setVisibility(View.VISIBLE);
            tvNoDetails.setText("Không có chi tiết đặt vé cho chuyến này");
            rvBookingDetails.setVisibility(View.GONE);
        } else {
            rvBookingDetails.setVisibility(View.VISIBLE);
            tvNoDetails.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    private void updateTripSpinner(List<Map<String, Object>> details) {
        // Lấy danh sách trip_id duy nhất từ dữ liệu
        Set<String> tripIds = new HashSet<>();
        for (Map<String, Object> booking : details) {
            Object tripId = booking.get("trip_id");
            if (tripId != null) {
                tripIds.add(tripId.toString());
            }
        }

        // Tạo danh sách với "Tất cả chuyến" ở đầu
        List<String> tripList = new ArrayList<>();
        tripList.add("Tất cả chuyến");
        tripList.addAll(new ArrayList<>(tripIds));

        // Setup adapter cho spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tripList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrip.setAdapter(adapter);

        // Hiển thị spinner nếu có nhiều hơn 1 chuyến
        if (tripList.size() > 1) {
            tripFilterContainer.setVisibility(View.VISIBLE);
        } else {
            tripFilterContainer.setVisibility(View.GONE);
        }
    }

    private void fetchBookingDetails(String groupBy, String value) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        int userId = sessionManager.getUserId();
        String refundType = getIntent().getStringExtra("refundType");
        if (refundType == null) refundType = "";
        // ✅ Lấy payment_method từ intent
        String paymentMethod = getIntent().getStringExtra("paymentMethod");
        if (paymentMethod == null) paymentMethod = "all";
        // ✅ Lấy operator từ intent
        String operator = getIntent().getStringExtra("operator");
        if (operator == null) operator = null;

        // Gọi API khác nhau tùy theo mode
        Call<List<Map<String, Object>>> call;
        if (isRefund) {
            // ✅ Truyền operator parameter cho hoàn tiền
            call = api.getRevenueRefundDetails(userId, groupBy, value, refundType, paymentMethod, operator);
        } else {
            // ✅ Truyền operator parameter
            call = api.getRevenueDetails(userId, groupBy, value, paymentMethod, operator);
        }

        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    allBookingDetails.clear();
                    allBookingDetails.addAll(response.body());

                    // Reset spinner
                    selectedTripId = null;
                    if (spinnerTrip.getAdapter() != null) {
                        spinnerTrip.setSelection(0);
                    }

                    // Update spinner với danh sách trip mới
                    updateTripSpinner(allBookingDetails);

                    // Hiển thị dữ liệu
                    filterBookingDetails();
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
