package vn.hcmute.busbooking.activity.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.EditUserActivity;
import vn.hcmute.busbooking.adapter.UsersAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.User;
import vn.hcmute.busbooking.utils.SessionManager;

public class ManageUsersActivity extends AppCompatActivity implements UsersAdapter.OnUserClickListener {

    private RecyclerView rvUsers;
    private ProgressBar progressUsers;
    private TextView tvEmptyUsers;
    private UsersAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        rvUsers = findViewById(R.id.rvUsers);
        progressUsers = findViewById(R.id.progressUsers);
        tvEmptyUsers = findViewById(R.id.tvEmptyUsers);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsersAdapter(userList, this);
        rvUsers.setAdapter(adapter);

        fetchAllUsers();
    }

    private void fetchAllUsers() {
        progressUsers.setVisibility(View.VISIBLE);
        tvEmptyUsers.setVisibility(View.GONE);

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
    public void onEditUser(User user) {
        Intent intent = new Intent(this, EditUserActivity.class);
        intent.putExtra("user", user);
        startActivity(intent);
    }

    @Override
    public void onDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa người dùng '" + user.getName() + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    int adminId = sessionManager.getUserId();
                    Call<Void> call = apiService.deleteUser(adminId, user.getId());
                    call.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(ManageUsersActivity.this, "Xóa người dùng thành công", Toast.LENGTH_SHORT).show();
                                fetchAllUsers(); // Refresh the list
                            } else {
                                Toast.makeText(ManageUsersActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(ManageUsersActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
