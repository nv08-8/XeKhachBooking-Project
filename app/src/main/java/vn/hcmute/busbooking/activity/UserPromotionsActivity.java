package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class UserPromotionsActivity extends AppCompatActivity {

    private RecyclerView rvPromotions;
    private PromotionsAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_user_promotions);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvPromotions = findViewById(R.id.rvPromotions);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        rvPromotions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PromotionsAdapter(new ArrayList<>());
        rvPromotions.setAdapter(adapter);

        apiService = ApiClient.getClient().create(ApiService.class);
        
        loadPromotions();
    }

    private void loadPromotions() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getPromotions().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Promotion> promotions = new ArrayList<>();
                    for (Map<String, Object> map : response.body()) {
                        // Sử dụng constructor có sẵn để tự động tạo title/description từ dữ liệu DB
                        Promotion p = new Promotion(map);
                        promotions.add(p);
                    }
                    
                    if (promotions.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        adapter.updateData(promotions);
                    }
                } else {
                    Toast.makeText(UserPromotionsActivity.this, "Không thể tải mã giảm giá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(UserPromotionsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}