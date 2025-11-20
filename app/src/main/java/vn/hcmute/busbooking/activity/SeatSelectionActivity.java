package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Build; // Added for API level check
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.SeatSelectionAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Seat;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class SeatSelectionActivity extends AppCompatActivity {

    private RecyclerView seatRecyclerView;
    private TextView tvTripDetails, tvSelectedSeatsInfo;
    private Button btnConfirmSeat;
    private ImageButton backButton;
    private ProgressBar progressBar;

    private SeatSelectionAdapter seatAdapter;
    private ApiService apiService;
    private SessionManager sessionManager;

    private Trip trip;
    private final List<Seat> seatList = new ArrayList<>();
    private final Set<String> selectedSeats = new HashSet<>();
    private int seatPrice = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        // Initialize views
        seatRecyclerView = findViewById(R.id.seatRecyclerView);
        tvTripDetails = findViewById(R.id.tvTripDetails);
        tvSelectedSeatsInfo = findViewById(R.id.tvSelectedSeatsInfo);
        btnConfirmSeat = findViewById(R.id.btnConfirmSeat);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.progressBar);

        // Get trip from intent (Parcelable-safe)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            trip = getIntent().getParcelableExtra("trip", Trip.class);
        } else {
            trip = getIntent().getParcelableExtra("trip");
        }

        if (trip == null) {
            Toast.makeText(this, "Không tìm thấy thông tin chuyến đi", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        seatPrice = (int) trip.getPrice();

        // Display trip details
        String tripInfo = trip.getOrigin() + " → " + trip.getDestination() + "\n" +
                         trip.getDepartureTime() + " - " + trip.getArrivalTime() + "\n" +
                         "Nhà xe: " + trip.getOperator();
        tvTripDetails.setText(tripInfo);

        // Setup RecyclerView
        seatRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        seatAdapter = new SeatSelectionAdapter(seatList, this::onSeatSelected);
        seatRecyclerView.setAdapter(seatAdapter);

        backButton.setOnClickListener(v -> finish());
        btnConfirmSeat.setOnClickListener(v -> confirmBooking());

        loadSeats();
    }

    private void loadSeats() {
        progressBar.setVisibility(View.VISIBLE);

        apiService.getSeats(trip.getId(), null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    seatList.clear();

                    // 1. Parse dữ liệu từ API
                    for (Map<String, Object> seatData : response.body()) {
                        Seat seat = new Seat();
                        String label = (String) seatData.get("label"); // Ví dụ: A01, B01...
                        seat.setLabel(label);

                        Object isBookedObj = seatData.get("is_booked");
                        boolean isBooked = false;
                        if (isBookedObj instanceof Boolean) {
                            isBooked = (Boolean) isBookedObj;
                        } else if (isBookedObj instanceof Number) {
                            isBooked = ((Number) isBookedObj).intValue() == 1;
                        }
                        seat.setBooked(isBooked);

                        seatList.add(seat);
                    }

                    // 2. SẮP XẾP LẠI DANH SÁCH ĐỂ HIỂN THỊ THEO CỘT DỌC (A B C D)
                    // 2. SẮP XẾP LẠI DANH SÁCH
                    // Logic: Tách phần chữ (A) và phần số (1, 10) để sắp xếp đúng thứ tự tự nhiên
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        seatList.sort((s1, s2) -> {
                            String label1 = s1.getLabel(); // VD: A1, A10
                            String label2 = s2.getLabel(); // VD: A2

                            // Tách phần chữ cái đầu tiên
                            String char1 = label1.replaceAll("[0-9]", "");
                            String char2 = label2.replaceAll("[0-9]", "");

                            // Tách phần số (dùng regex lấy số)
                            String numStr1 = label1.replaceAll("[^0-9]", "");
                            String numStr2 = label2.replaceAll("[^0-9]", "");

                            int num1 = numStr1.isEmpty() ? 0 : Integer.parseInt(numStr1);
                            int num2 = numStr2.isEmpty() ? 0 : Integer.parseInt(numStr2);

                            // --- LOGIC SẮP XẾP THEO CỘT DỌC (A B C D) ---
                            // Muốn A1, B1, C1, D1 nằm ngang hàng nhau trên Grid 4 cột
                            // Thì thứ tự trong List phải là: A1, B1, C1, D1, A2, B2, C2, D2...

                            // Ưu tiên so sánh số ghế trước (1 vs 1, 1 vs 2)
                            int numCompare = Integer.compare(num1, num2);
                            if (numCompare != 0) {
                                return numCompare;
                            }

                            // Nếu số ghế bằng nhau (cùng là hàng 1), so sánh chữ cái (A vs B)
                            return char1.compareTo(char2);
                        });
                    }


                    seatAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(SeatSelectionActivity.this, "Không thể tải danh sách ghế", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SeatSelectionActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void onSeatSelected(Seat seat) {
        if (selectedSeats.contains(seat.getLabel())) {
            selectedSeats.remove(seat.getLabel());
        } else {
            selectedSeats.add(seat.getLabel());
        }
        updateSelectedInfo();
    }

    private void updateSelectedInfo() {
        int totalAmount = selectedSeats.size() * seatPrice;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        String info = "Đã chọn: " + selectedSeats.size() + " ghế\n" +
                     "Tổng tiền: " + formatter.format(totalAmount);
        tvSelectedSeatsInfo.setText(info);

        btnConfirmSeat.setEnabled(!selectedSeats.isEmpty());
    }

    private void confirmBooking() {
        if (selectedSeats.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt vé", Toast.LENGTH_SHORT).show();
            // Redirect to login
            Intent intent = new Intent(this, vn.hcmute.busbooking.activity.LoginActivity.class);
            startActivity(intent);
            return;
        }

        // ⭐ LẤY DANH SÁCH TẤT CẢ CÁC GHẾ ĐÃ CHỌN
        List<String> seatsToBook = new ArrayList<>(selectedSeats);
        int totalAmount = selectedSeats.size() * seatPrice;

        progressBar.setVisibility(View.VISIBLE);
        btnConfirmSeat.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("trip_id", trip.getId());

        // ⭐ GỬI MẢNG GHẾ LÊN SERVER (Sử dụng key: "seat_labels" như đã sửa trong Backend)
        body.put("seat_labels", seatsToBook);

        apiService.createBooking(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                btnConfirmSeat.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();

                    // ⭐ XỬ LÝ DANH SÁCH BOOKING ID TRẢ VỀ TỪ SERVER
                    Object bookingIdsObj = result.get("booking_ids");
                    if (!(bookingIdsObj instanceof List)) {
                        Toast.makeText(SeatSelectionActivity.this, "Lỗi định dạng phản hồi đặt vé", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<Double> doubleIds = (List<Double>) bookingIdsObj;
                    List<Integer> bookingIds = new ArrayList<>();
                    for (Double id : doubleIds) {
                        bookingIds.add(id.intValue());
                    }

                    Toast.makeText(SeatSelectionActivity.this, "Đặt " + seatsToBook.size() + " vé thành công!", Toast.LENGTH_SHORT).show();

                    // Chuyển sang PaymentActivity
                    Intent intent = new Intent(SeatSelectionActivity.this, PaymentActivity.class);

                    // ⭐ TRUYỀN DANH SÁCH BOOKING ID VÀ TỔNG TIỀN ĐẾN PAYMENT ACTIVITY
                    intent.putIntegerArrayListExtra("booking_ids", (ArrayList<Integer>) bookingIds);
                    intent.putStringArrayListExtra("seat_labels", (ArrayList<String>) seatsToBook);
                    intent.putExtra("amount", totalAmount);
                    intent.putExtra("origin", trip.getOrigin());
                    intent.putExtra("destination", trip.getDestination());
                    intent.putExtra("operator", trip.getOperator());

                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SeatSelectionActivity.this, "Đặt vé thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnConfirmSeat.setEnabled(true);
                Toast.makeText(SeatSelectionActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
