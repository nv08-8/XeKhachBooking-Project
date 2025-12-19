package vn.hcmute.busbooking.activity.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.admin.PromotionsAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Promotion;
import vn.hcmute.busbooking.utils.SessionManager;

public class ManagePromotionsActivity extends AppCompatActivity implements PromotionsAdapter.OnPromotionClickListener {

    private RecyclerView rvPromotions;
    private PromotionsAdapter adapter;
    private List<Promotion> promotionList = new ArrayList<>();
    private ApiService apiService;
    private SessionManager sessionManager;
    private ProgressBar progressPromotions;
    private TextView tvEmptyPromotions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_promotions);

        rvPromotions = findViewById(R.id.rvPromotions);
        FloatingActionButton fabAddPromotion = findViewById(R.id.fabAddPromotion);
        progressPromotions = findViewById(R.id.progressPromotions);
        tvEmptyPromotions = findViewById(R.id.tvEmptyPromotions);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        setupRecyclerView();
        fetchPromotions();

        fabAddPromotion.setOnClickListener(v -> {
            Intent intent = new Intent(ManagePromotionsActivity.this, PromotionFormActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPromotions();
    }

    private void setupRecyclerView() {
        adapter = new PromotionsAdapter(promotionList, this);
        rvPromotions.setLayoutManager(new LinearLayoutManager(this));
        rvPromotions.setAdapter(adapter);
    }

    private void fetchPromotions() {
        progressPromotions.setVisibility(View.VISIBLE);
        rvPromotions.setVisibility(View.GONE);
        tvEmptyPromotions.setVisibility(View.GONE);

        apiService.getAdminPromotions(sessionManager.getUserId()).enqueue(new Callback<List<Promotion>>() {
            @Override
            public void onResponse(Call<List<Promotion>> call, Response<List<Promotion>> response) {
                progressPromotions.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    promotionList.clear();
                    promotionList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (promotionList.isEmpty()) {
                        tvEmptyPromotions.setVisibility(View.VISIBLE);
                    } else {
                        rvPromotions.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmptyPromotions.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Promotion>> call, Throwable t) {
                progressPromotions.setVisibility(View.GONE);
                tvEmptyPromotions.setVisibility(View.VISIBLE);
                Toast.makeText(ManagePromotionsActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditPromotion(Promotion promotion) {
        Intent intent = new Intent(this, PromotionFormActivity.class);
        intent.putExtra("promotion_id", promotion.getId());
        startActivity(intent);
    }

    @Override
    public void onDeletePromotion(Promotion promotion) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa khuyến mãi " + promotion.getCode() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> deletePromotion(promotion.getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deletePromotion(int promotionId) {
        apiService.deletePromotion(sessionManager.getUserId(), promotionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ManagePromotionsActivity.this, "Xóa khuyến mãi thành công", Toast.LENGTH_SHORT).show();
                    fetchPromotions();
                } else {
                    Toast.makeText(ManagePromotionsActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ManagePromotionsActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
