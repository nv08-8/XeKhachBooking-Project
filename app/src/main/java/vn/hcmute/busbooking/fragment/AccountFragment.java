package vn.hcmute.busbooking.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.GuestHomeActivity;
import vn.hcmute.busbooking.activity.LoginActivity;
import vn.hcmute.busbooking.utils.SessionManager;

public class AccountFragment extends Fragment {

    private TextView tvUserName, tvUserEmail;
    private ListView lvAccountOptions;
    private Button btnLogout;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        lvAccountOptions = view.findViewById(R.id.lvAccountOptions);
        btnLogout = view.findViewById(R.id.btnLogout);

        sessionManager = new SessionManager(getContext());

        if (sessionManager.isLoggedIn()) {
            tvUserName.setText(sessionManager.getUserName());
            tvUserEmail.setText(sessionManager.getUserEmail());
        } else {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }

        List<String> options = new ArrayList<>();
        options.add("Điểm thưởng của tôi");
        options.add("Ưu đãi");
        options.add("Giới thiệu nhận quà");
        options.add("Quản lý thẻ");
        options.add("Đánh giá chuyến đi");
        options.add("Cài đặt");
        options.add("Trung tâm Hỗ trợ");
        options.add("Góp ý");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, options);
        lvAccountOptions.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(getActivity(), GuestHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }
}
