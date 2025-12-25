package vn.hcmute.busbooking.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import vn.hcmute.busbooking.R;

public class LoadingUtil {

    private static Dialog loadingDialog;

    public static void showLoading(Context context) {
        showLoading(context, null);
    }

    public static void showLoading(Context context, String message) {
        // Hide any existing dialog to prevent duplicates
        hideLoading();

        if (context == null) return;

        loadingDialog = new Dialog(context);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.dialog_loading);

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvLoadingMessage = loadingDialog.findViewById(R.id.tvLoadingMessage);
        if (!TextUtils.isEmpty(message)) {
            tvLoadingMessage.setText(message);
            tvLoadingMessage.setVisibility(View.VISIBLE);
        } else {
            tvLoadingMessage.setVisibility(View.GONE);
        }

        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);

        try {
            loadingDialog.show();
        } catch (Exception e) {
            // Handle exceptions like WindowManager$BadTokenException if the context is invalid
            e.printStackTrace();
        }
    }

    public static void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss();
            } catch (Exception e) {
                // Handle exceptions that might occur on dismissal
                e.printStackTrace();
            }
        }
        loadingDialog = null;
    }
}
