package vn.hcmute.busbooking.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.BookingDetailActivity;
import vn.hcmute.busbooking.activity.ChangePasswordActivity;
import vn.hcmute.busbooking.activity.EditProfileActivity;
import vn.hcmute.busbooking.activity.FeedbackManagementActivity;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.MyBookingsActivity;
import vn.hcmute.busbooking.activity.MyCoinsActivity;
import vn.hcmute.busbooking.activity.PersonalInfoActivity;
import vn.hcmute.busbooking.activity.RegisterActivity;
import vn.hcmute.busbooking.activity.UserPromotionsActivity;
import vn.hcmute.busbooking.api.ApiClient;
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

            // Hide status bar scrim and bottom nav card in fragment if needed (since they belong to activity)
            View statusBarScrim = view.findViewById(R.id.statusBarScrim);
            View bottomNavCard = view.findViewById(R.id.bottomNavCard);
            if (statusBarScrim != null) statusBarScrim.setVisibility(View.GONE);
            if (bottomNavCard != null) bottomNavCard.setVisibility(View.GONE);

            ImageView ivUserAvatar = view.findViewById(R.id.ivUserAvatar);
            TextView tvUserName = view.findViewById(R.id.tvUserName);
            TextView tvUserEmail = view.findViewById(R.id.tvUserEmail);
            
            TextView tvMyOffers = view.findViewById(R.id.tvMyOffers);
            TextView tvMyFeedbacks = view.findViewById(R.id.tvMyFeedbacks);
            TextView tvMyCoins = view.findViewById(R.id.tvMyCoins);
            TextView tvPersonalInfo = view.findViewById(R.id.tvPersonalInfo);
            TextView tvChangePassword = view.findViewById(R.id.tvChangePassword);
            TextView tvLogout = view.findViewById(R.id.tvLogout);

            // Set Data
            tvUserName.setText(sessionManager.getUserName());
            tvUserEmail.setText(sessionManager.getUserEmail());
            loadAvatarImage(ivUserAvatar);

            // Listeners
            if (tvMyOffers != null) {
                tvMyOffers.setOnClickListener(v -> startActivity(new Intent(getActivity(), UserPromotionsActivity.class)));
            }

            if (tvMyFeedbacks != null) {
                tvMyFeedbacks.setOnClickListener(v -> startActivity(new Intent(getActivity(), FeedbackManagementActivity.class)));
            }

            if (tvMyCoins != null) {
                tvMyCoins.setOnClickListener(v -> startActivity(new Intent(getActivity(), MyCoinsActivity.class)));
            }

            if (tvPersonalInfo != null) {
                tvPersonalInfo.setOnClickListener(v -> startActivity(new Intent(getActivity(), PersonalInfoActivity.class)));
            }

            if (tvChangePassword != null) {
                tvChangePassword.setOnClickListener(v -> startActivity(new Intent(getActivity(), ChangePasswordActivity.class)));
            }

            if (tvLogout != null) {
                tvLogout.setOnClickListener(v -> {
                    sessionManager.clearSession();
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.recreate(); // refresh to guest view
                    }
                });
            }

            return view;
        } else {
            // Guest: show login/register actions
            View view = inflater.inflate(R.layout.activity_guest_account, container, false);

            Button btnLogin = view.findViewById(R.id.btnLogin);
            Button btnRegister = view.findViewById(R.id.btnRegister);

            if (btnLogin != null) btnLogin.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoginActivity.class)));
            if (btnRegister != null) btnRegister.setOnClickListener(v -> startActivity(new Intent(getActivity(), RegisterActivity.class)));

            return view;
        }
    }

    private void loadAvatarImage(ImageView ivUserAvatar) {
        if (ivUserAvatar == null) return;
        String avatarUrl = sessionManager.getUserAvatar();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            String fullUrl = avatarUrl.startsWith("/") ? ApiClient.getBaseUrl() + avatarUrl : avatarUrl;
            Glide.with(this)
                    .load(fullUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(ivUserAvatar);
        } else {
            ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder);
        }
    }
}
