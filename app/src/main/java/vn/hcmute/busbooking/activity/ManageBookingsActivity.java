package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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
import vn.hcmute.busbooking.adapter.BookingsAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class ManageBookingsActivity extends AppCompatActivity {
    private ProgressBar progressBookings;
    private RecyclerView rvBookings;
    private TextView tvEmptyBookings;
    private Spinner spinnerBookingStatus;
    private Button btnRefreshBookings;
    private BookingsAdapter adapter;
    private List<Map<String, Object>> bookingsList = new ArrayList<>();
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_bookings);

        sessionManager = new SessionManager(this);

        progressBookings = findViewById(R.id.progressBookings);
        rvBookings = findViewById(R.id.rvBookings);
        tvEmptyBookings = findViewById(R.id.tvEmptyBookings);
        spinnerBookingStatus = findViewById(R.id.spinnerBookingStatus);
        btnRefreshBookings = findViewById(R.id.btnRefreshBookings);

        // Setup RecyclerView
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingsAdapter(bookingsList, new BookingsAdapter.OnBookingClickListener() {
            @Override
            public void onConfirmBooking(Map<String, Object> booking) {
                Toast.makeText(ManageBookingsActivity.this, "Xác nhận: " + booking.get("id"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelBooking(Map<String, Object> booking) {
                Toast.makeText(ManageBookingsActivity.this, "Hủy: " + booking.get("id"), Toast.LENGTH_SHORT).show();
            }
        });
        rvBookings.setAdapter(adapter);

        btnRefreshBookings.setOnClickListener(v -> fetchAdminBookings());
        spinnerBookingStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                fetchAdminBookings();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        fetchAdminBookings();
    }

    private void fetchAdminBookings() {
        progressBookings.setVisibility(View.VISIBLE);
        rvBookings.setVisibility(View.GONE);
        tvEmptyBookings.setVisibility(View.GONE);

        ApiService api = ApiClient.getClient().create(ApiService.class);

        int userId = sessionManager.getUserId();
        String status = spinnerBookingStatus.getSelectedItem().toString();
        // Giả sử mục đầu tiên là "Tất cả" và được ánh xạ tới một chuỗi trống cho API
        if (spinnerBookingStatus.getSelectedItemPosition() == 0) {
            status = "";
        }

        Call<List<Map<String, Object>>> call = api.getAdminBookings(userId, null, null, status, 1, 50);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                progressBookings.setVisibility(View.GONE);
                if (!response.isSuccessful()) {
                    tvEmptyBookings.setText("Không thể tải dữ liệu (code=" + response.code() + ")");
                    tvEmptyBookings.setVisibility(View.VISIBLE);
                    return;
                }

                List<Map<String, Object>> bookings = response.body();

                if (bookings == null || bookings.isEmpty()) {
                    tvEmptyBookings.setText("Không có đơn đặt vé");
                    tvEmptyBookings.setVisibility(View.VISIBLE);
                } else {
                    bookingsList.clear();
                    bookingsList.addAll(bookings);
                    adapter.notifyDataSetChanged();
                    rvBookings.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                progressBookings.setVisibility(View.GONE);
                tvEmptyBookings.setText("Lỗi: " + t.getMessage());
                tvEmptyBookings.setVisibility(View.VISIBLE);
            }
        });
    }
}
