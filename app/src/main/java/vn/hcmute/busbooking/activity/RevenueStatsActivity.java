package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;

public class RevenueStatsActivity extends AppCompatActivity {

    private TextView tvStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_stats);

        tvStats = findViewById(R.id.tvStats);
        tvStats.setText("Báo cáo doanh thu (mock)\n- Theo tuyến\n- Theo ngày/tháng/năm");
    }
}

