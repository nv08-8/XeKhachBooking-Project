package vn.hcmute.busbooking.fragment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.TripListActivity;
import vn.hcmute.busbooking.activity.UserAccountActivity;
import vn.hcmute.busbooking.adapter.TripAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class HomeFragment extends Fragment {

    private AutoCompleteTextView etOrigin, etDestination;
    private TextView tvDate, tvWelcome;
    private ImageButton btnSwap;
    private Button btnSearchTrips;
    private RecyclerView rvPopularRoutes;

    private ApiService apiService;
    private TripAdapter tripAdapter;
    private SessionManager sessionManager;
    private final Calendar myCalendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find Views
        etOrigin = view.findViewById(R.id.etOrigin);
        etDestination = view.findViewById(R.id.etDestination);
        tvDate = view.findViewById(R.id.tvDate);
        btnSwap = view.findViewById(R.id.btnSwap);
        btnSearchTrips = view.findViewById(R.id.btnSearchTrips);
        rvPopularRoutes = view.findViewById(R.id.rvPopularRoutes);
        tvWelcome = view.findViewById(R.id.tvWelcome);

        // Initialize
        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(getContext());

        // Setup Welcome Message
        if (sessionManager.isLoggedIn()) {
            tvWelcome.setText("Chào " + sessionManager.getUserName() + " >");
        }

        // Setup RecyclerView
        rvPopularRoutes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        tripAdapter = new TripAdapter(new ArrayList<>());
        rvPopularRoutes.setAdapter(tripAdapter);

        setupClickListeners();

        // Populate Data
        String[] locations = {"TP.HCM", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Nha Trang", "Buôn Ma Thuột"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, locations);
        etOrigin.setAdapter(adapter);
        etDestination.setAdapter(adapter);

        fetchTrips();
    }

    private void setupClickListeners() {
        // Date Picker
        DatePickerDialog.OnDateSetListener date = (view, year, month, day) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, day);
            updateLabel();
        };
        tvDate.setOnClickListener(v -> new DatePickerDialog(getContext(), date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show());

        // Swap origin and destination
        btnSwap.setOnClickListener(v -> {
            String origin = etOrigin.getText().toString();
            String destination = etDestination.getText().toString();
            etOrigin.setText(destination);
            etDestination.setText(origin);
        });

        // Search Button
        btnSearchTrips.setOnClickListener(v -> {
            String from = etOrigin.getText().toString().trim();
            String to = etDestination.getText().toString().trim();
            String dateStr = tvDate.getText().toString().trim();

            if (from.isEmpty() || to.isEmpty() || dateStr.isEmpty() || dateStr.equals("Ngày đi")) {
                Toast.makeText(getContext(), "Vui lòng chọn đủ điểm đi, điểm đến và ngày đi", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(getActivity(), TripListActivity.class);
            intent.putExtra("from", from);
            intent.putExtra("to", to);
            intent.putExtra("date", dateStr);
            startActivity(intent);
        });

        // Adapter item click
        tripAdapter.setOnItemClickListener(trip -> {
            DatePickerDialog.OnDateSetListener datePicker = (view, year, month, day) -> {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, day);
                String selectedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(myCalendar.getTime());

                Intent intent = new Intent(getActivity(), TripListActivity.class);
                intent.putExtra("from", trip.getFromLocation());
                intent.putExtra("to", trip.getToLocation());
                intent.putExtra("date", selectedDate);
                startActivity(intent);
            };
            new DatePickerDialog(getContext(), datePicker, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        tvWelcome.setOnClickListener(v -> {
            Intent intent;
            if (sessionManager.isLoggedIn()) {
                intent = new Intent(getActivity(), UserAccountActivity.class);
            } else {
                intent = new Intent(getActivity(), LoginActivity.class);
            }
            startActivity(intent);
        });
    }

    private void updateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(myFormat, Locale.US);
        tvDate.setText(dateFormat.format(myCalendar.getTime()));
    }

    private void fetchTrips() {
        apiService.getTrips(null, null).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tripAdapter.updateTrips(response.body());
                } else {
                    Toast.makeText(getContext(), "Failed to fetch trips", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("HomeFragment", "Error fetching trips", t);
            }
        });
    }
}
