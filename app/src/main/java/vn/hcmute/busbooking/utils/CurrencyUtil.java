package vn.hcmute.busbooking.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyUtil {

    /**
     * Format số tiền thành chuỗi với đơn vị VND
     * Ví dụ: 250000 -> "250.000 VND"
     */
    public static String formatVND(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');

        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount) + " VND";
    }

    /**
     * Format số tiền thành chuỗi với đơn vị VND (số nguyên)
     * Ví dụ: 250000 -> "250.000 VND"
     */
    public static String formatVND(int amount) {
        return formatVND((double) amount);
    }

    /**
     * Format số tiền thành chuỗi với đơn vị VND (long)
     * Ví dụ: 250000L -> "250.000 VND"
     */
    public static String formatVND(long amount) {
        return formatVND((double) amount);
    }
}

