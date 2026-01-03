package vn.hcmute.busbooking.adapter.admin;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.BookingViewHolder> {

    private List<Map<String, Object>> bookings;
    private OnBookingClickListener listener;

    public interface OnBookingClickListener {
        void onBookingClicked(Map<String, Object> booking);
        void onConfirmBooking(Map<String, Object> booking);
        void onCancelBooking(Map<String, Object> booking);
    }

    public BookingsAdapter(List<Map<String, Object>> bookings, OnBookingClickListener listener) {
        this.bookings = bookings;
        this.listener = listener;
    }

    @Override
    public BookingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_booking, parent, false);
        return new BookingViewHolder(view);
    }

    private String formatDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            Date date = sdf.parse(dateTimeStr);
            SimpleDateFormat newSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            return newSdf.format(date);
        } catch (ParseException e) {
            return dateTimeStr;
        }
    }

    @Override
    public void onBindViewHolder(BookingViewHolder holder, int position) {
        Map<String, Object> booking = bookings.get(position);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookingClicked(booking);
            }
        });

        Object idObj = booking.get("id");
        Object userNameObj = booking.get("name");
        Object originObj = booking.get("origin");
        Object destObj = booking.get("destination");
        Object departureTimeObj = booking.get("departure_time");
        Object priceObj = booking.get("total_amount");
        Object statusObj = booking.get("status");
        Object cancellationMessageObj = booking.get("cancellation_message");
        Object tripStatusObj = booking.get("trip_status");

        holder.tvBookingId.setText("Đơn #" + (idObj != null ? idObj.toString() : "?"));
        holder.tvUserName.setText(userNameObj != null ? userNameObj.toString() : "");
        holder.tvBookingRoute.setText((originObj != null ? originObj.toString() : "?") + " → " + (destObj != null ? destObj.toString() : "?"));
        holder.tvDepartureTime.setText("Khởi hành: " + (departureTimeObj != null ? formatDateTime(departureTimeObj.toString()) : ""));

        String priceString = "0";
        if (priceObj != null) {
            try {
                double price = Double.parseDouble(priceObj.toString());
                priceString = String.format("%,.0f", price);
            } catch (NumberFormatException e) {
                priceString = priceObj.toString();
            }
        }
        holder.tvBookingPrice.setText(priceString + " VNĐ");

        // Set status with styling
        if (statusObj != null) {
            setStatus(holder, statusObj.toString());
        }

        // Lấy giá trị price_paid để kiểm tra vé đã thanh toán hay chưa
        Object pricePaidObj = booking.get("price_paid");
        double pricePaid = 0;
        try {
            if (pricePaidObj != null) {
                pricePaid = Double.parseDouble(pricePaidObj.toString());
            }
        } catch (NumberFormatException e) {
            pricePaid = 0;
        }

        // Kiểm tra: chỉ làm nhạt vé đã thanh toán nhưng bị hủy
        // Vé chưa thanh toán bị hủy thì hiển thị bình thường
        boolean isCancelledButPaid = statusObj != null && statusObj.toString().equals("cancelled") && pricePaid > 0;
        boolean isConfirmedButTripCancelled = statusObj != null && statusObj.toString().equals("confirmed") &&
                                              tripStatusObj != null && tripStatusObj.toString().equals("cancelled");

        if (isCancelledButPaid || isConfirmedButTripCancelled) {
            // Làm nhạt item (vé đã thanh toán nhưng bị hủy)
            holder.itemView.setAlpha(0.6f);

            // Hiển thị thông báo hủy nếu có
            if (cancellationMessageObj != null && !cancellationMessageObj.toString().isEmpty()) {
                holder.tvCancellationMessage.setText("⚠️ " + cancellationMessageObj.toString());
                holder.tvCancellationMessage.setVisibility(View.VISIBLE);
            } else {
                holder.tvCancellationMessage.setVisibility(View.GONE);
            }
        } else {
            // Hiển thị bình thường (vé chưa thanh toán hoặc không hủy)
            holder.itemView.setAlpha(1.0f);
            holder.tvCancellationMessage.setVisibility(View.GONE);
        }
    }

    private void setStatus(BookingViewHolder holder, String status) {
        Context context = holder.itemView.getContext();
        String statusText;
        Drawable background;

        switch (status.toLowerCase()) {
            case "pending":
                statusText = "Chờ thanh toán";
                background = ContextCompat.getDrawable(context, R.drawable.bg_status_pending);
                break;
            case "confirmed":
                statusText = "Đã thanh toán";
                background = ContextCompat.getDrawable(context, R.drawable.bg_status_confirmed);
                break;
            case "completed":
                statusText = "Đã đi";
                background = ContextCompat.getDrawable(context, R.drawable.bg_status_confirmed);
                break;
            case "cancelled":
                statusText = "Đã hủy";
                background = ContextCompat.getDrawable(context, R.drawable.bg_status_cancelled);
                break;
            case "expired":
                statusText = "Đã hủy";
                background = ContextCompat.getDrawable(context, R.drawable.bg_status_cancelled);
                break;
            default:
                statusText = status;
                background = ContextCompat.getDrawable(context, R.drawable.bg_status_pending);
                break;
        }

        holder.tvBookingStatus.setText(statusText);
        holder.tvBookingStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        holder.statusBadge.setBackground(background);
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId, tvUserName, tvBookingRoute, tvDepartureTime, tvBookingPrice, tvBookingStatus, tvCancellationMessage;
        View statusBadge;
        // The views below are kept to avoid crashing the app, but are not used directly.
        TextView tvBookingSeat;
        Button btnConfirmBooking, btnCancelBooking;

        public BookingViewHolder(View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvBookingRoute = itemView.findViewById(R.id.tvBookingRoute);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvBookingPrice = itemView.findViewById(R.id.tvBookingPrice);
            tvCancellationMessage = itemView.findViewById(R.id.tvCancellationMessage);
            tvBookingStatus = itemView.findViewById(R.id.tvStatus);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            // The following views are found but are not styled or used directly in the adapter logic
            // to adhere to the new design, but they are kept to prevent crashes.
            tvBookingSeat = itemView.findViewById(R.id.tvBookingSeat);
            btnConfirmBooking = itemView.findViewById(R.id.btnConfirmBooking);
            btnCancelBooking = itemView.findViewById(R.id.btnCancelBooking);
        }
    }
}
