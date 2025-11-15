package vn.hcmute.busbooking.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.MyBookingsActivity;
import vn.hcmute.busbooking.utils.SessionManager;

public class UserAccountFragment extends Fragment {

    private TextView tvUserName;
    private SessionManager sessionManager;
    private Button btnMyBookings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_user_account, container, false);

        tvUserName = view.findViewById(R.id.tvUserName);
        sessionManager = new SessionManager(getContext());
        btnMyBookings = view.findViewById(R.id.btnMyBookings);

        String userName = sessionManager.getUserName();
        if (userName != null) {
            tvUserName.setText(userName);
        }

        btnMyBookings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MyBookingsActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }
}
