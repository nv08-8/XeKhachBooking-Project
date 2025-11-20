package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;

public class ManageRoutesActivity extends AppCompatActivity {

    private ListView listView;
    private Button btnAddRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_routes);

        listView = findViewById(R.id.lvRoutes);
        btnAddRoute = findViewById(R.id.btnAddRoute);

        // Sample placeholder data until API is wired
        String[] sample = new String[]{"Hà Nội - Hải Phòng", "Hải Phòng - Quảng Ninh", "Hà Nội - Nam Định"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sample);
        listView.setAdapter(adapter);

        btnAddRoute.setOnClickListener(v -> {
            // TODO: mở RouteFormActivity để thêm tuyến
            Toast.makeText(ManageRoutesActivity.this, "Mở form thêm tuyến (chưa triển khai)", Toast.LENGTH_SHORT).show();
        });
    }
}

