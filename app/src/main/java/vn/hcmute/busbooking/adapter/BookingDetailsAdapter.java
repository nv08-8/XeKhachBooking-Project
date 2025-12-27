package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            Date date = sdf.parse(dateTimeStr);
            SimpleDateFormat newSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            return newSdf.format(date);
        } catch (ParseException e) {
            return dateTimeStr; // Return original if parsing fails
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> detail = bookingDetails.get(position);

        holder.tvBookingId.setText("Mã đặt vé: #" + detail.get("booking_id").toString());
        holder.tvUserName.setText(detail.get("user_name").toString());
        holder.tvRouteInfo.setText("Tuyến: " + detail.get("route_info").toString());
        holder.tvDepartureTime.setText("Khởi hành: " + formatDateTime(detail.get("departure_time").toString()));
        holder.tvTicketCount.setText("Số vé: " + detail.get("ticket_count").toString());
        holder.tvTotalPrice.setText("Tổng tiền: " + detail.get("total_price").toString() + " VNĐ");
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
