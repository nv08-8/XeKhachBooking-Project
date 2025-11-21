package vn.hcmute.busbooking.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.hcmute.busbooking.R;

public class PaymentWebActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_web);

        String checkoutUrl = getIntent().getStringExtra("checkoutUrl");
        if (checkoutUrl == null || checkoutUrl.isEmpty()) {
            Toast.makeText(this, "Link thanh toán không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        webView = findViewById(R.id.webViewPayment);
        progressBar = findViewById(R.id.progressBar);

        // Configure WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);

                // Check if redirecting to app deep link
                if (url.startsWith("xekhachbooking://payment")) {
                    handlePaymentDeepLink(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);

                // Check again for deep link in case of redirect
                if (url.startsWith("xekhachbooking://payment")) {
                    handlePaymentDeepLink(url);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Handle deep link redirect
                if (url.startsWith("xekhachbooking://payment")) {
                    handlePaymentDeepLink(url);
                    return true;
                }

                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PaymentWebActivity.this, "Lỗi tải trang thanh toán", Toast.LENGTH_SHORT).show();
            }
        });

        progressBar.setVisibility(View.VISIBLE);
        webView.loadUrl(checkoutUrl);
    }

    private void handlePaymentDeepLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

