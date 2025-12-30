package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class BookingDetailsAdapter extends RecyclerView.Adapter<BookingDetailsAdapter.ViewHolder> {

    private List<Map<String, Object>> bookingDetails;

    public BookingDetailsAdapter(List<Map<String, Object>> bookingDetails) {
        this.bookingDetails = bookingDetails;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_detail, parent, false);
        return new ViewHolder(view);
    }

    private String formatDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return "";
        try {
            Date date = null;
            if (dateTimeStr.contains("T")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                date = sdf.parse(dateTimeStr);
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                date = sdf.parse(dateTimeStr);
            }
            
            if (date != null) {
                SimpleDateFormat newSdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
                return newSdf.format(date);
            }
        } catch (Exception e) {
            // Fallback for messy strings
            if (dateTimeStr.length() >= 10) return dateTimeStr.substring(0, 10);
        }
        return dateTimeStr;
    }

    private String formatCurrency(Object amountObj) {
        if (amountObj == null) return "0 VNĐ";
        try {
            double amount = Double.parseDouble(amountObj.toString());
            DecimalFormat formatter = new DecimalFormat("#,###");
            return formatter.format(amount) + " VNĐ";
        } catch (Exception e) {
            return amountObj.toString() + " VNĐ";
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> detail = bookingDetails.get(position);

        holder.tvBookingId.setText("#" + detail.get("booking_id").toString());
        holder.tvUserName.setText(detail.get("user_name").toString());
        holder.tvRouteInfo.setText(detail.get("route_info").toString());
        holder.tvDepartureTime.setText(formatDateTime(detail.get("departure_time").toString()));
        holder.tvTicketCount.setText(detail.get("ticket_count").toString() + " vé");
        holder.tvTotalPrice.setText(formatCurrency(detail.get("total_price")));
    }

    @Override
    public int getItemCount() {
        return bookingDetails.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId, tvUserName, tvRouteInfo, tvDepartureTime, tvTicketCount, tvTotalPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvRouteInfo = itemView.findViewById(R.id.tvRouteInfo);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvTicketCount = itemView.findViewById(R.id.tvTicketCount);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
        }
    }
}
