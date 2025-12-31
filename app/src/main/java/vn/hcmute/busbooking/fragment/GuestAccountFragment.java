package vn.hcmute.busbooking.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.activity.RegisterActivity;

public class GuestAccountFragment extends Fragment {

    private static final String TAG = "GuestAccountFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_guest_account, container, false);

        Button btnLogin = view.findViewById(R.id.btnLogin);
        Button btnRegister = view.findViewById(R.id.btnRegister);

        Log.d(TAG, "onCreateView - btnLogin=" + (btnLogin != null) + ", btnRegister=" + (btnRegister != null));

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                Log.d(TAG, "btnLogin clicked");
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });
        }

        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                Log.d(TAG, "btnRegister clicked");
                Intent intent = new Intent(getActivity(), RegisterActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }
}
