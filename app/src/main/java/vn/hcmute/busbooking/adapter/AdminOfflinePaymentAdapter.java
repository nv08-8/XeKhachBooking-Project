package vn.hcmute.busbooking.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import vn.hcmute.busbooking.R;

public class AdminOfflinePaymentAdapter extends RecyclerView.Adapter<AdminOfflinePaymentAdapter.ViewHolder> {

    private List<Map<String, Object>> bookings;
    private OnConfirmListener onConfirmListener;

    public interface OnConfirmListener {
        void onConfirm(Map<String, Object> booking);
    }

    public AdminOfflinePaymentAdapter(List<Map<String, Object>> bookings, OnConfirmListener listener) {
        this.bookings = bookings;
        this.onConfirmListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_offline_payment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> booking = bookings.get(position);
        holder.bind(booking, onConfirmListener);
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBookingId, tvUserName, tvUserPhone, tvRoute, tvAmount, tvCreatedTime, tvPaymentMethod, tvSeats;
        private final Button btnConfirm;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserPhone = itemView.findViewById(R.id.tvUserPhone);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCreatedTime = itemView.findViewById(R.id.tvCreatedTime);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvSeats = itemView.findViewById(R.id.tvSeats);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
        }

        public void bind(Map<String, Object> booking, OnConfirmListener listener) {
            // Booking ID
            int bookingId = ((Number) booking.get("id")).intValue();
            tvBookingId.setText("Vé #" + bookingId);

            // User info
            String userName = (String) booking.get("user_name");
            String userPhone = (String) booking.get("user_phone");
            tvUserName.setText(userName != null ? userName : "N/A");
            tvUserPhone.setText(userPhone != null ? userPhone : "N/A");

            // Route info
            String origin = (String) booking.get("origin");
            String destination = (String) booking.get("destination");
            tvRoute.setText((origin != null ? origin : "N/A") + " → " + (destination != null ? destination : "N/A"));

            // Amount
            double amount = ((Number) booking.get("total_amount")).doubleValue();
            tvAmount.setText(String.format(new Locale("vi", "VN"), "%,.0f đ", amount));

            // Created time
            String createdAt = (String) booking.get("created_at");
            tvCreatedTime.setText("Tạo: " + formatDateTime(createdAt));

            // Payment method
            String paymentMethod = (String) booking.get("payment_method");
            tvPaymentMethod.setText("PT: " + (paymentMethod != null ? paymentMethod : "N/A"));

            // Seats
            Object seatsObj = booking.get("seat_labels");
            String seatsText = "Ghế: ";
            if (seatsObj instanceof List) {
                List<?> seatList = (List<?>) seatsObj;
                seatsText += String.join(", ", seatList.stream().map(Object::toString).toArray(String[]::new));
            } else {
                seatsText += "N/A";
            }
            tvSeats.setText(seatsText);

            // Confirm button
            btnConfirm.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConfirm(booking);
                }
            });
        }

        private String formatDateTime(String isoString) {
            if (isoString == null) return "";
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = isoFormat.parse(isoString);
                if (date == null) return "";
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
                return dateFormat.format(date);
            } catch (Exception e) {
                return "";
            }
        }
    }
}

