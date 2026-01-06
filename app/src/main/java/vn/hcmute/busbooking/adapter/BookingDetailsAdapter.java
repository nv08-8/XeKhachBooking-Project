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
            // Try parsing with milliseconds and Z first (UTC format)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(dateTimeStr);

            // Convert UTC to Vietnam timezone (+7)
            SimpleDateFormat newSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            newSdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+7"));
            return newSdf.format(date);
        } catch (ParseException e) {
            try {
                // Try parsing server format: YYYY-MM-DD HH:MI:SS (already in Vietnam time)
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                Date date = sdf.parse(dateTimeStr);
                SimpleDateFormat newSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                // No timezone conversion needed - server already sent Vietnam time
                return newSdf.format(date);
            } catch (ParseException e2) {
                try {
                    // Try parsing without milliseconds - ISO format (UTC)
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    Date date = sdf.parse(dateTimeStr);
                    SimpleDateFormat newSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                    return newSdf.format(date);
                } catch (ParseException e3) {
                    return dateTimeStr; // Return original if parsing fails
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> detail = bookingDetails.get(position);

        // Debug log
        android.util.Log.d("BookingDetailsAdapter", "Position " + position + " - Detail: " + detail.toString());

        // Null-safe getter helper
        String bookingId = detail.get("booking_id") != null ? detail.get("booking_id").toString() : "";
        String userName = detail.get("user_name") != null ? detail.get("user_name").toString() : "";
        String routeInfo = detail.get("route_info") != null ? detail.get("route_info").toString() : "";
        String departureTime = detail.get("departure_time") != null ? detail.get("departure_time").toString() : "";
        String ticketCount = detail.get("ticket_count") != null ? detail.get("ticket_count").toString() : "";
        String totalPrice = detail.get("total_price") != null ? detail.get("total_price").toString() : "";
        String refundAmount = detail.get("refund_amount") != null ? detail.get("refund_amount").toString() : "";
        String paidAt = detail.get("paid_at") != null ? detail.get("paid_at").toString() : "";

        android.util.Log.d("BookingDetailsAdapter", "totalPrice=" + totalPrice + ", refundAmount=" + refundAmount);

        holder.tvBookingId.setText("Mã đặt vé: #" + bookingId);
        holder.tvUserName.setText(userName);
        holder.tvRouteInfo.setText("Tuyến: " + routeInfo);
        holder.tvDepartureTime.setText("Khởi hành: " + formatDateTime(departureTime));

        // Hiển thị số vé - nếu không có ticket_count thì hiển thị 1 (mặc định)
        String displayTicketCount = !ticketCount.isEmpty() ? ticketCount : "1";
        holder.tvTicketCount.setText("Số vé: " + displayTicketCount);

        // Dùng total_price nếu có, nếu không dùng refund_amount (cho hoàn tiền)
        // Nếu cả hai đều trống, hiển thị 0
        String priceDisplay = !totalPrice.isEmpty() ? totalPrice : (!refundAmount.isEmpty() ? refundAmount : "0");
        holder.tvTotalPrice.setText("Tổng tiền: " + priceDisplay + " VNĐ");

        // Hiển thị thời gian thanh toán
        if (!paidAt.isEmpty()) {
            holder.tvPaymentTime.setText("Thanh toán: " + formatDateTime(paidAt));
        } else {
            holder.tvPaymentTime.setText("Thanh toán: -");
        }
    }

    @Override
    public int getItemCount() {
        return bookingDetails.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId, tvUserName, tvRouteInfo, tvDepartureTime, tvTicketCount, tvTotalPrice, tvPaymentTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvRouteInfo = itemView.findViewById(R.id.tvRouteInfo);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvTicketCount = itemView.findViewById(R.id.tvTicketCount);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvPaymentTime = itemView.findViewById(R.id.tvPaymentTime);
        }
    }
}
