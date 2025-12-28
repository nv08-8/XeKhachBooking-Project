package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.FaqAdapter;
import vn.hcmute.busbooking.model.FaqItem;

public class HelpCenterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_help_center);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvFaq = findViewById(R.id.rvFaq);
        rvFaq.setLayoutManager(new LinearLayoutManager(this));

        String[] questions = getResources().getStringArray(R.array.faq_questions);
        String[] answers = getResources().getStringArray(R.array.faq_answers);

        List<FaqItem> faqItems = new ArrayList<>();
        for (int i = 0; i < questions.length; i++) {
            faqItems.add(new FaqItem(questions[i], answers[i]));
        }

        FaqAdapter adapter = new FaqAdapter(faqItems);
        rvFaq.setAdapter(adapter);

        handleWindowInsets();
    }

    private void handleWindowInsets() {
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        View statusBarScrim = findViewById(R.id.statusBarScrim);

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
