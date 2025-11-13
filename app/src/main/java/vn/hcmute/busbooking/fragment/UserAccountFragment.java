package vn.hcmute.busbooking.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.utils.SessionManager;

public class UserAccountFragment extends Fragment {

    private TextView tvUserName;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_user, container, false);

        tvUserName = view.findViewById(R.id.tvUserName);
        sessionManager = new SessionManager(getContext());

        String userName = sessionManager.getUserName();
        if (userName != null) {
            tvUserName.setText(userName);
        }

        return view;
    }
}
