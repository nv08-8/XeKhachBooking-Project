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
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.RegisterActivity;
import vn.hcmute.busbooking.utils.SessionManager;

public class AccountFragment extends Fragment {

    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sessionManager = new SessionManager(getContext());
        View view;

        if (sessionManager.getUserId() != null) {
            // User is logged in
            view = inflater.inflate(R.layout.fragment_account_user, container, false);
            TextView tvUserName = view.findViewById(R.id.tvUserName);
            String userName = sessionManager.getUserName();
            if (userName != null) {
                tvUserName.setText("Xin chào, " + userName);
            }
            // You can add a logout button and other user-specific functionality here
        } else {
            // User is a guest
            view = inflater.inflate(R.layout.fragment_account_user, container, false);
            Button btnLogin = view.findViewById(R.id.btnLogin);
            Button btnRegister = view.findViewById(R.id.btnRegister);

            btnLogin.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });

            btnRegister.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), RegisterActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }
}
