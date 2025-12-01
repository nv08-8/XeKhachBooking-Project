package vn.hcmute.busbooking.activity.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.TripFormActivity;
import vn.hcmute.busbooking.adapter.admin.TripsAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class ManageTripsActivity extends AppCompatActivity implements TripsAdapter.OnTripClickListener {

    private RecyclerView rvTrips;
    private ProgressBar progressTrips;
    private TextView tvEmptyTrips;
    private Button btnAddTrip;
    private TripsAdapter adapter;
    private List<Trip> tripList = new ArrayList<>();
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_trips);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        rvTrips = findViewById(R.id.rvTrips);
        progressTrips = findViewById(R.id.progressTrips);
        tvEmptyTrips = findViewById(R.id.tvEmptyTrips);
        btnAddTrip = findViewById(R.id.btnAddTrip);

        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TripsAdapter(tripList, this);
        rvTrips.setAdapter(adapter);

        btnAddTrip.setOnClickListener(v -> {
            Intent intent = new Intent(ManageTripsActivity.this, TripFormActivity.class);
            startActivity(intent);
        });

        fetchTrips();
    }

    private void fetchTrips() {
        progressTrips.setVisibility(View.VISIBLE);
        tvEmptyTrips.setVisibility(View.GONE);

        Call<List<Trip>> call = apiService.getTrips(null, null, null);
        call.enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                progressTrips.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    tripList.clear();
                    tripList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (tripList.isEmpty()) {
                        tvEmptyTrips.setVisibility(View.VISIBLE);
                    } else {
                        rvTrips.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmptyTrips.setText("Không thể tải danh sách chuyến đi");
                    tvEmptyTrips.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                progressTrips.setVisibility(View.GONE);
                tvEmptyTrips.setText("Lỗi: " + t.getMessage());
                tvEmptyTrips.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onEditTrip(Trip trip) {
        Intent intent = new Intent(this, TripFormActivity.class);
        intent.putExtra("trip", trip);
        startActivity(intent);
    }

    @Override
    public void onDeleteTrip(Trip trip) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa chuyến đi này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    int adminId = sessionManager.getUserId();
                    Call<Map<String, Object>> call = apiService.deleteTrip(adminId, trip.getId());
                    call.enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(ManageTripsActivity.this, "Xóa thành công", Toast.LENGTH_SHORT).show();
                                fetchTrips();
                            } else {
                                Toast.makeText(ManageTripsActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            Toast.makeText(ManageTripsActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
