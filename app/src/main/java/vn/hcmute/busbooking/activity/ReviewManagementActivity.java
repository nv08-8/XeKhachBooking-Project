package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.ReviewAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Feedback;
import vn.hcmute.busbooking.utils.SessionManager;

public class ReviewManagementActivity extends AppCompatActivity implements ReviewAdapter.OnReviewActionListener {

    private RecyclerView rvReviews;
    private ReviewAdapter adapter;
    private List<Feedback> feedbackList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmptyMessage;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_management);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onSupportNavigateUp());

        // Find Views
        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        // Setup RecyclerView
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(feedbackList, this);
        rvReviews.setAdapter(adapter);

        // Load feedbacks
        loadAllFeedbacks();
    }

    private void loadAllFeedbacks() {
        progressBar.setVisibility(View.VISIBLE);
        rvReviews.setVisibility(View.GONE);
        tvEmptyMessage.setVisibility(View.GONE);

        // ✅ Gọi getAllFeedbacks thay vì getReviewedFeedbacks để lấy tất cả feedback
        apiService.getAllFeedbacks().enqueue(new Callback<List<Feedback>>() {
            @Override
            public void onResponse(Call<List<Feedback>> call, Response<List<Feedback>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    feedbackList.clear();
                    feedbackList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (feedbackList.isEmpty()) {
                        tvEmptyMessage.setText("Chưa có feedback nào");
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                    } else {
                        rvReviews.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmptyMessage.setText("Không thể tải dữ liệu");
                    tvEmptyMessage.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Feedback>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmptyMessage.setText("Lỗi: " + t.getMessage());
                tvEmptyMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onEdit(Feedback feedback) {
        showEditDialog(feedback);
    }

    @Override
    public void onDelete(Feedback feedback) {
        showDeleteConfirmDialog(feedback);
    }

    @Override
    public void onReply(Feedback feedback) {
        showReplyDialog(feedback);
    }

    private void showEditDialog(Feedback feedback) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_feedback_edit, null);
        EditText etComment = dialogView.findViewById(R.id.etComment);
        etComment.setText(feedback.getComment());

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa feedback")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String updatedComment = etComment.getText().toString();
                    updateFeedback(feedback.getId(), updatedComment);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmDialog(Feedback feedback) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa feedback")
                .setMessage("Bạn có chắc muốn xóa feedback này?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteFeedback(feedback.getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showReplyDialog(Feedback feedback) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_feedback_reply, null);
        EditText etReply = dialogView.findViewById(R.id.etReply);

        new AlertDialog.Builder(this)
                .setTitle("Trả lời feedback")
                .setView(dialogView)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String reply = etReply.getText().toString();
                    if (reply.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập nội dung trả lời", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendReply(feedback.getId(), reply);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateFeedback(int feedbackId, String comment) {
        Map<String, Object> body = new HashMap<>();
        body.put("comment", comment);

        apiService.updateFeedback(feedbackId, body).enqueue(new Callback<Feedback>() {
            @Override
            public void onResponse(Call<Feedback> call, Response<Feedback> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ReviewManagementActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    loadAllFeedbacks();
                } else {
                    Toast.makeText(ReviewManagementActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Feedback> call, Throwable t) {
                Toast.makeText(ReviewManagementActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteFeedback(int feedbackId) {
        apiService.deleteFeedback(feedbackId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ReviewManagementActivity.this, "Xóa thành công", Toast.LENGTH_SHORT).show();
                    loadAllFeedbacks();
                } else {
                    Toast.makeText(ReviewManagementActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ReviewManagementActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendReply(int feedbackId, String reply) {
        Map<String, Object> body = new HashMap<>();
        body.put("reply", reply);

        apiService.replyFeedback(feedbackId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ReviewManagementActivity.this, "Gửi trả lời thành công", Toast.LENGTH_SHORT).show();
                    loadAllFeedbacks();
                } else {
                    Toast.makeText(ReviewManagementActivity.this, "Gửi trả lời thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ReviewManagementActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

