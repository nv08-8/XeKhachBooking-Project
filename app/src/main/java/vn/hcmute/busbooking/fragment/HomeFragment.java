package vn.hcmute.busbooking.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.GuestHomeActivity;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.TripListActivity;
import vn.hcmute.busbooking.utils.SessionManager;

public class HomeFragment extends Fragment {

    private AutoCompleteTextView etOrigin, etDestination;
    private Button btnSearchTrips;
    private TextView tvWelcome, tvLogin;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views and SessionManager
        etOrigin = view.findViewById(R.id.etOrigin);
        etDestination = view.findViewById(R.id.etDestination);
        btnSearchTrips = view.findViewById(R.id.btnSearchTrips);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvLogin = view.findViewById(R.id.tvLogin);
        sessionManager = new SessionManager(getContext());

        // Update UI based on login status
        updateUI();

        // Setup AutoCompleteTextViews
        String[] locations = {"TP.HCM", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Nha Trang", "Buôn Ma Thuột"};
        if(getContext() != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, locations);
            etOrigin.setAdapter(adapter);
            etDestination.setAdapter(adapter);
        }

        btnSearchTrips.setOnClickListener(v -> {
            String from = etOrigin.getText().toString();
            String to = etDestination.getText().toString();

            Intent intent = new Intent(getActivity(), TripListActivity.class);
            intent.putExtra("origin", from);
            intent.putExtra("destination", to);
            startActivity(intent);
        });
    }

    private void updateUI() {
        String userName = sessionManager.getUserName();
        // Check if user is logged in (userName is not null and not "null")
        if (userName != null && !userName.equalsIgnoreCase("null")) {
            // User is logged in
            tvWelcome.setText("Xin chào, " + userName + "!");
            tvLogin.setText("Đăng xuất");
            tvLogin.setOnClickListener(v -> {
                // Perform logout
                sessionManager.logout();
                Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();

                // Go to GuestHomeActivity
                Intent intent = new Intent(getActivity(), GuestHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            });
        } else {
            // User is not logged in (Guest mode - although this fragment is for logged in users)
            tvWelcome.setText("Xin chào, Khách!");
            tvLogin.setText("Đăng nhập");
            tvLogin.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });
        }
    }
}
