package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;

public class RouteFormActivity extends AppCompatActivity {

    private EditText edtOrigin, edtDestination, edtPrice, edtDuration;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_form);

        edtOrigin = findViewById(R.id.edtOrigin);
        edtDestination = findViewById(R.id.edtDestination);
        edtPrice = findViewById(R.id.edtPrice);
        edtDuration = findViewById(R.id.edtDuration);
        btnSave = findViewById(R.id.btnSaveRoute);

        btnSave.setOnClickListener(v -> {
            // TODO: call API to save route
            Toast.makeText(RouteFormActivity.this, "Lưu tuyến (chưa triển khai)", Toast.LENGTH_SHORT).show();
        });
    }
}

