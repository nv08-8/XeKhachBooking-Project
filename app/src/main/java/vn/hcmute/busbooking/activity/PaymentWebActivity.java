package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import vn.hcmute.busbooking.R;

public class PaymentWebActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_web);

        String checkoutUrl = getIntent().getStringExtra("checkoutUrl");
        if (checkoutUrl == null) {
            Toast.makeText(this, "Checkout url missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        webView = findViewById(R.id.webViewPayment);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("xekhachbooking://payment")) {
                    handlePaymentDeepLink(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // If the gateway redirects to our app scheme it won't render; handle here as fallback
                if (url.startsWith("xekhachbooking://payment")) {
                    handlePaymentDeepLink(url);
                }
            }
        });

        webView.loadUrl(checkoutUrl);
    }

    private void handlePaymentDeepLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        finish();
    }
}

