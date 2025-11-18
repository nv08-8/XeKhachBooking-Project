package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.utils.SessionManager;

public class UserAccountActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserMembership;
    private ListView lvAccountOptions;
    private Button btnLogout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_account);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserMembership = findViewById(R.id.tvUserMembership);
        lvAccountOptions = findViewById(R.id.lvAccountOptions);
        btnLogout = findViewById(R.id.btnLogout);

        sessionManager = new SessionManager(this);

        // Populate User Info
        if (sessionManager.isLoggedIn()) {
            tvUserName.setText(sessionManager.getUserName());
            // You can add more logic for membership level later
            tvUserMembership.setText("Thành viên Mới"); 
        } else {
            // Redirect to login if not logged in
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Populate ListView
        List<String> options = new ArrayList<>();
        options.add("Điểm thưởng của tôi");
        options.add("Ưu đãi");
        options.add("Giới thiệu nhận quà");
        options.add("Quản lý thẻ");
        options.add("Đánh giá chuyến đi");
        options.add("Cài đặt");
        options.add("Trung tâm Hỗ trợ");
        options.add("Góp ý");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, options);
        lvAccountOptions.setAdapter(adapter);

        // Logout Button
        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(this, GuestHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
