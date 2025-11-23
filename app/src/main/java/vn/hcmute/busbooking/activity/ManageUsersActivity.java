package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.UsersAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.User;
import vn.hcmute.busbooking.utils.SessionManager;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private ProgressBar progressUsers;
    private TextView tvEmptyUsers;
    private UsersAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sessionManager = new SessionManager(this);

        rvUsers = findViewById(R.id.rvUsers);
        progressUsers = findViewById(R.id.progressUsers);
        tvEmptyUsers = findViewById(R.id.tvEmptyUsers);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsersAdapter(userList);
        rvUsers.setAdapter(adapter);

        fetchAllUsers();
    }

    private void fetchAllUsers() {
        progressUsers.setVisibility(View.VISIBLE);
        tvEmptyUsers.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        int userId = sessionManager.getUserId();

        Call<List<User>> call = apiService.getAllUsers(userId);
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                progressUsers.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    userList.clear();
                    userList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (userList.isEmpty()) {
                        tvEmptyUsers.setVisibility(View.VISIBLE);
                    } else {
                        rvUsers.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmptyUsers.setText("Không thể tải danh sách người dùng");
                    tvEmptyUsers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                progressUsers.setVisibility(View.GONE);
                tvEmptyUsers.setText("Lỗi: " + t.getMessage());
                tvEmptyUsers.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
