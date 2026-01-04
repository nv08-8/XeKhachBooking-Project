package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.utils.SessionManager;

public class MyCoinsActivity extends AppCompatActivity {

    private TextView tvCoinBalance;
    private RecyclerView rvCoinHistory;
    private HistoryAdapter adapter;
    private List<Map<String, Object>> historyList = new ArrayList<>();
    private ApiService apiService;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_coins);

        SessionManager sessionManager = new SessionManager(this);
        Integer id = sessionManager.getUserId();
        if (id == null) { finish(); return; }
        userId = id;

        tvCoinBalance = findViewById(R.id.tvCoinBalance);
        rvCoinHistory = findViewById(R.id.rvCoinHistory);
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        rvCoinHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        rvCoinHistory.setAdapter(adapter);

        apiService = ApiClient.getClient().create(ApiService.class);
        loadCoinData();
    }

    private void loadCoinData() {
        // Lấy số dư
        apiService.getCoinBalance(userId).enqueue(new Callback<Map<String, Integer>>() {
            @Override
            public void onResponse(Call<Map<String, Integer>> call, Response<Map<String, Integer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Integer balance = response.body().get("balance");
                    tvCoinBalance.setText(String.format("%,d", balance != null ? balance : 0));
                }
            }
            @Override
            public void onFailure(Call<Map<String, Integer>> call, Throwable t) {}
        });

        // Lấy lịch sử
        apiService.getCoinHistory(userId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historyList.clear();
                    historyList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_coin_history, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = historyList.get(position);
            holder.tvDescription.setText((String) item.get("description"));
            holder.tvDate.setText((String) item.get("created_at"));
            
            double amount = ((Number) item.get("amount")).doubleValue();
            if (amount > 0) {
                holder.tvAmount.setText("+" + String.format("%,.0f", amount));
                holder.tvAmount.setTextColor(getResources().getColor(R.color.Green));
            } else {
                holder.tvAmount.setText(String.format("%,.0f", amount));
                holder.tvAmount.setTextColor(getResources().getColor(R.color.errorRed));
            }
        }

        @Override
        public int getItemCount() { return historyList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDescription, tvDate, tvAmount;
            ViewHolder(View v) {
                super(v);
                tvDescription = v.findViewById(R.id.tvDescription);
                tvDate = v.findViewById(R.id.tvDate);
                tvAmount = v.findViewById(R.id.tvAmount);
            }
        }
    }
}
