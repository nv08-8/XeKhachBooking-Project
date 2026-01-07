package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class RevenueAdapter extends RecyclerView.Adapter<RevenueAdapter.RevenueViewHolder> {

    private List<Map<String, Object>> revenues;
    private OnItemClickListener listener;
    private boolean isRefund = false; // true = hoàn tiền, false = doanh thu
    private boolean isAppProfit = false; // ✅ true = lợi nhuận app

    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> revenue);
    }

    public RevenueAdapter(List<Map<String, Object>> revenues, OnItemClickListener listener) {
        this.revenues = revenues;
        this.listener = listener;
    }

    public RevenueAdapter(List<Map<String, Object>> revenues, OnItemClickListener listener, boolean isRefund) {
        this.revenues = revenues;
        this.listener = listener;
        this.isRefund = isRefund;
    }

    // ✅ Constructor mới để hỗ trợ lợi nhuận app
    public RevenueAdapter(List<Map<String, Object>> revenues, OnItemClickListener listener, boolean isRefund, boolean isAppProfit) {
        this.revenues = revenues;
        this.listener = listener;
        this.isRefund = isRefund;
        this.isAppProfit = isAppProfit;
    }

    @Override
    public RevenueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_revenue, parent, false);
        return new RevenueViewHolder(view);
    }

    private String formatDateValue(Object dateObj) {
        if (dateObj == null) return "Không rõ ngày";
        String dateStr = dateObj.toString().trim();

        // Debug
        android.util.Log.d("formatDateValue", "Input: " + dateStr + " (len=" + dateStr.length() + ")");

        try {
            Date date = null;

            // Handle DATE type từ API (yyyy-MM-dd dạng string)
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                date = sdf.parse(dateStr);
                SimpleDateFormat outFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                String result = outFormat.format(date);
                android.util.Log.d("formatDateValue", "Matched yyyy-MM-dd, result: " + result);
                return result;
            }

            // Handle YYYY-MM format (month)
            if (dateStr.matches("\\d{4}-\\d{2}")) {
                android.util.Log.d("formatDateValue", "Matched YYYY-MM");
                return dateStr;
            }

            // Handle YYYY format (year) - có thể là string hoặc số
            if (dateStr.matches("\\d{4}$")) {
                android.util.Log.d("formatDateValue", "Matched YYYY");
                return dateStr;
            }

            // Handle ISO datetime format (T và Z)
            if (dateStr.contains("T")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                date = sdf.parse(dateStr);
            } else if (dateStr.contains(" ")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                date = sdf.parse(dateStr);
            }

            if (date != null) {
                // Nếu có giờ trong dateStr, hiển thị cả giờ
                if (dateStr.contains("T") || dateStr.contains(" ")) {
                    SimpleDateFormat newSdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
                    return newSdf.format(date);
                } else {
                    SimpleDateFormat newSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                    return newSdf.format(date);
                }
            }
        } catch (Exception e) {
            android.util.Log.d("formatDateValue", "Exception: " + e.getMessage());
            // Fallback: If it's yyyy-MM-dd... just take the first 10 chars and reformat manually
            if (dateStr.length() >= 10 && dateStr.charAt(4) == '-' && dateStr.charAt(7) == '-') {
                String y = dateStr.substring(0, 4);
                String m = dateStr.substring(5, 7);
                String d = dateStr.substring(8, 10);
                return d + "/" + m + "/" + y;
            }
        }
        android.util.Log.d("formatDateValue", "Return as-is: " + dateStr);
        return dateStr;
    }

    private String formatCurrency(Object amountObj) {
        if (amountObj == null) return "0 VND";
        try {
            double amount = Double.parseDouble(amountObj.toString());
            DecimalFormat formatter = new DecimalFormat("#,###");
            return formatter.format(amount) + " VND";
        } catch (Exception e) {
            return amountObj.toString() + " VND";
        }
    }

    @Override
    public void onBindViewHolder(RevenueViewHolder holder, int position) {
        Map<String, Object> revenue = revenues.get(position);

        Object groupKey = revenue.get("group_key");
        // Debug log
        android.util.Log.d("RevenueAdapter", "Position " + position + " - groupKey: " + groupKey + " (type: " + (groupKey != null ? groupKey.getClass().getSimpleName() : "null") + ")");

        // Hỗ trợ cả total_revenue (doanh thu) và refund_amount (hoàn tiền)
        Object totalRev = revenue.get("total_revenue");
        if (totalRev == null) {
            totalRev = revenue.get("refund_amount");
        }
        Object totalBookings = revenue.get("total_bookings");
        
        String title = "?";
        String date = groupKey != null ? formatDateValue(groupKey) : "?";

        // Check if this is a route/trip grouping (has origin/destination)
        if (revenue.containsKey("origin") && revenue.containsKey("destination")) {
            title = revenue.get("origin") + " - " + revenue.get("destination");
            if (revenue.containsKey("departure_time")) {
                date = formatDateValue(revenue.get("departure_time"));
            }
        } else {
            // For date/month/year grouping, use group_key as title
            if (isAppProfit) {
                title = "Báo cáo lợi nhuận App"; // ✅ Lợi nhuận App
            } else if (isRefund) {
                title = "Báo cáo hoàn tiền";
            } else {
                title = "Báo cáo doanh thu";
            }
            // date đã được set ở trên từ formatDateValue(groupKey)
        }

        holder.tvRevenueTitle.setText(title);
        holder.tvRevenueDate.setText(date);

        // ✅ Hiển thị app_revenue khi isAppProfit, hoàn tiền khi isRefund, doanh thu bình thường
        Object amountToDisplay = null;
        if (isAppProfit) {
            amountToDisplay = revenue.get("app_revenue"); // Hiển thị lợi nhuận app
        } else if (isRefund) {
            amountToDisplay = revenue.get("refund_amount"); // Hiển thị hoàn tiền
        } else {
            amountToDisplay = revenue.get("total_revenue"); // Hiển thị doanh thu
        }
        holder.tvRevenueAmount.setText(formatCurrency(amountToDisplay));

        // Format tickets without .0 by casting to long
        long ticketCount = totalBookings != null ? getLongFromObject(totalBookings) : 0;
        holder.tvRevenueTickets.setText(ticketCount + " vé");

        // ✅ Update label to show "Lợi nhuận", "Hoàn tiền" or "Doanh thu"
        if (isAppProfit) {
            holder.tvRevenueLabel.setText("Lợi nhuận");
        } else {
            holder.tvRevenueLabel.setText(isRefund ? "Hoàn tiền" : "Doanh thu");
        }

        // ✅ Hiển thị loại hoàn tiền khi là refund mode
        if (isRefund && revenue.containsKey("admin_cancelled_count") && revenue.containsKey("trip_cancelled_count")) {
            long adminCount = getLongFromObject(revenue.get("admin_cancelled_count"));
            long tripCount = getLongFromObject(revenue.get("trip_cancelled_count"));
            long userCount = getLongFromObject(revenue.get("user_cancelled_count"));

            String refundTypeText = "Tất cả";
            if (adminCount > 0 && tripCount == 0 && userCount == 0) {
                refundTypeText = "Admin hủy";
            } else if (tripCount > 0 && adminCount == 0 && userCount == 0) {
                refundTypeText = "Chuyến hủy";
            } else if (userCount > 0 && adminCount == 0 && tripCount == 0) {
                refundTypeText = "User hủy";
            }
            holder.tvRefundType.setText(refundTypeText);
        } else {
            holder.tvRefundType.setText("-");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(revenue);
            }
        });
    }

    private long getLongFromObject(Object obj) {
        if (obj == null) return 0;
        try {
            if (obj instanceof Long) return (Long) obj;
            if (obj instanceof Integer) return ((Integer) obj).longValue();
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return revenues != null ? revenues.size() : 0;
    }

    public static class RevenueViewHolder extends RecyclerView.ViewHolder {
        TextView tvRevenueTitle, tvRevenueDate, tvRevenueAmount, tvRevenueTickets, tvRevenueLabel, tvRefundType;

        public RevenueViewHolder(View itemView) {
            super(itemView);
            tvRevenueTitle = itemView.findViewById(R.id.tvRevenueTitle);
            tvRevenueDate = itemView.findViewById(R.id.tvRevenueDate);
            tvRevenueAmount = itemView.findViewById(R.id.tvRevenueAmount);
            tvRevenueTickets = itemView.findViewById(R.id.tvRevenueTickets);
            tvRevenueLabel = itemView.findViewById(R.id.tvRevenueLabel);
            tvRefundType = itemView.findViewById(R.id.tvRefundType);
        }
    }
}
