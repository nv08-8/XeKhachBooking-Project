package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.PromotionsAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Promotion;

public class MyOffersActivity extends AppCompatActivity {

    private PromotionsAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout llEmptyState;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_offers);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        RecyclerView rvOffers = findViewById(R.id.rvOffers);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);

        apiService = ApiClient.getClient().create(ApiService.class);

        if (rvOffers != null) {
            rvOffers.setLayoutManager(new LinearLayoutManager(this));
            adapter = new PromotionsAdapter(new ArrayList<>());
            rvOffers.setAdapter(adapter);
        }

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadPromotions);
        }

        loadPromotions();
    }

    private void loadPromotions() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        apiService.getPromotions().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Promotion> promotions = new ArrayList<>();
                    for (Map<String, Object> map : response.body()) {
                        // Sử dụng Constructor từ Map có sẵn trong model Promotion
                        // Nó sẽ tự động xử lý id, code, discount_type, discount_value...
                        Promotion p = new Promotion(map);
                        
                        // Nếu database trả về title/description trống, model sẽ tự tạo từ code
                        if (p.getTitle() == null || p.getTitle().isEmpty()) {
                            p.setTitle(p.getCode());
                        }
                        
                        promotions.add(p);
                    }
                    if (adapter != null) {
                        adapter.updateData(promotions);
                    }
                    if (llEmptyState != null) {
                        llEmptyState.setVisibility(promotions.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                } else {
                    Toast.makeText(MyOffersActivity.this, "Không thể tải danh sách ưu đãi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                Toast.makeText(MyOffersActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
