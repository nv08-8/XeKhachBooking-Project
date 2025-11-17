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
        sessionManager = new SessionManager(requireContext());

        if (sessionManager.getUserId() != null) {
            // Logged in: show user account
            View view = inflater.inflate(R.layout.activity_user_account, container, false);

            TextView tvUserName = view.findViewById(R.id.tvUserName);
            TextView tvUserEmail = view.findViewById(R.id.tvUserEmail);
            Button btnLogout = view.findViewById(R.id.btnLogout);
            Button btnEditProfile = view.findViewById(R.id.btnEditProfile);
            Button btnChangePassword = view.findViewById(R.id.btnChangePassword);

            String userName = sessionManager.getUserName();
            String userEmail = sessionManager.getUserEmail();
            if (userName != null) tvUserName.setText("Xin chào, " + userName);
            if (userEmail != null) tvUserEmail.setText(userEmail);


            if (btnEditProfile != null) {
                btnEditProfile.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), vn.hcmute.busbooking.activity.EditProfileActivity.class);
                    startActivity(intent);
                });
            }

            if (btnChangePassword != null) {
                btnChangePassword.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), vn.hcmute.busbooking.activity.ChangePasswordActivity.class);
                    startActivity(intent);
                });
            }

            btnLogout.setOnClickListener(v -> {
                sessionManager.clearSession();
                requireActivity().recreate(); // refresh to guest view
            });

            return view;
        } else {
            // Guest: show login/register actions
            View view = inflater.inflate(R.layout.activity_guest_account, container, false);

            Button btnLogin = view.findViewById(R.id.btnLogin);
            Button btnRegister = view.findViewById(R.id.btnRegister);

            btnLogin.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoginActivity.class)));
            btnRegister.setOnClickListener(v -> startActivity(new Intent(getActivity(), RegisterActivity.class)));

            return view;
        }
    }
}
