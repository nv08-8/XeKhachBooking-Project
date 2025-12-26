package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import vn.hcmute.busbooking.R;

public class LegalContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_legal_content);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView tvContent = findViewById(R.id.tvContent);
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        View statusBarScrim = findViewById(R.id.statusBarScrim);

        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");

        if (title != null) {
            toolbar.setTitle(title);
        }

        if (content != null) {
            tvContent.setText(content);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        handleWindowInsets(appBarLayout, statusBarScrim);
    }

    private void handleWindowInsets(AppBarLayout appBarLayout, View statusBarScrim) {
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = statusBarHeight;
            v.setLayoutParams(params);

            if (statusBarScrim != null) {
                ViewGroup.LayoutParams scrimParams = statusBarScrim.getLayoutParams();
                scrimParams.height = statusBarHeight;
                statusBarScrim.setLayoutParams(scrimParams);
                statusBarScrim.setVisibility(statusBarHeight > 0 ? View.VISIBLE : View.GONE);
            }

            return insets;
        });
    }
}