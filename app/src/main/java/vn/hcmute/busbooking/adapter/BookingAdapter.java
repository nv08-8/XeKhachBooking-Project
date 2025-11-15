package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Map<String, Object>> bookings;
    private OnCancelListener cancelListener;

    public interface OnCancelListener {
        void onCancel(Map<String, Object> booking);
    }

    public BookingAdapter(List<Map<String, Object>> bookings, OnCancelListener listener) {
        this.bookings = bookings;
        this.cancelListener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Map<String, Object> booking = bookings.get(position);

        String origin = (String) booking.get("origin");
        String destination = (String) booking.get("destination");
        String seatLabel = (String) booking.get("seat_label");
        String status = (String) booking.get("status");
        String departureTime = (String) booking.get("departure_time");
        Object priceObj = booking.get("price_paid");

        int price = 0;
        if (priceObj instanceof Double) {
            price = ((Double) priceObj).intValue();
        } else if (priceObj instanceof Integer) {
            price = (Integer) priceObj;
        }

        holder.tvRoute.setText(origin + " → " + destination);
        holder.tvSeat.setText("Ghế: " + seatLabel);
        holder.tvStatus.setText("Trạng thái: " + getStatusText(status));
        holder.tvDeparture.setText("Khởi hành: " + (departureTime != null ? departureTime : "N/A"));
        holder.tvPrice.setText(String.format("%,d VNĐ", price));

        // Show cancel button only for confirmed/pending bookings
        if ("confirmed".equals(status) || "pending".equals(status)) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> {
                if (cancelListener != null) {
                    cancelListener.onCancel(booking);
                }
            });
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void updateBookings(List<Map<String, Object>> newBookings) {
        this.bookings = newBookings;
        notifyDataSetChanged();
    }

    private String getStatusText(String status) {
        switch (status) {
            case "confirmed": return "Đã xác nhận";
            case "pending": return "Chờ xác nhận";
            case "cancelled": return "Đã hủy";
            case "refunded": return "Đã hoàn tiền";
            default: return status;
        }
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvSeat, tvStatus, tvDeparture, tvPrice;
        Button btnCancel;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvSeat = itemView.findViewById(R.id.tvSeat);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDeparture = itemView.findViewById(R.id.tvDeparture);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}

