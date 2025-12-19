package vn.hcmute.busbooking.adapter.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.BookingViewHolder> {

    private List<Map<String, Object>> bookings;
    private OnBookingClickListener listener;

    public interface OnBookingClickListener {
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

    @Override
    public void onBindViewHolder(BookingViewHolder holder, int position) {
        Map<String, Object> booking = bookings.get(position);

        Object idObj = booking.get("id");
        Object originObj = booking.get("origin");
        Object destObj = booking.get("destination");
        Object seatObj = booking.get("seat_label"); // Make sure your API returns this key
        Object priceObj = booking.get("total_price"); // Use total_price as per API response
        Object statusObj = booking.get("status");

        holder.tvBookingId.setText("Đơn #" + (idObj != null ? idObj.toString() : "?"));
        holder.tvBookingRoute.setText((originObj != null ? originObj.toString() : "?") + " → " + (destObj != null ? destObj.toString() : "?"));
        holder.tvBookingSeat.setText("Ghế: " + (seatObj != null ? seatObj.toString() : "?"));
        holder.tvBookingPrice.setText((priceObj != null ? priceObj.toString() : "0") + " VNĐ");
        holder.tvBookingStatus.setText(statusObj != null ? statusObj.toString() : "");

        // Handle button visibility based on status
        if ("pending".equalsIgnoreCase(String.valueOf(statusObj))) {
            holder.btnConfirmBooking.setVisibility(View.VISIBLE);
            holder.btnCancelBooking.setVisibility(View.VISIBLE);
        } else {
            holder.btnConfirmBooking.setVisibility(View.GONE);
            holder.btnCancelBooking.setVisibility(View.GONE);
        }

        holder.btnConfirmBooking.setOnClickListener(v -> {
            if (listener != null) listener.onConfirmBooking(booking);
        });

        holder.btnCancelBooking.setOnClickListener(v -> {
            if (listener != null) listener.onCancelBooking(booking);
        });
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId, tvBookingRoute, tvBookingSeat, tvBookingPrice, tvBookingStatus;
        Button btnConfirmBooking, btnCancelBooking;

        public BookingViewHolder(View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvBookingRoute = itemView.findViewById(R.id.tvBookingRoute);
            tvBookingSeat = itemView.findViewById(R.id.tvBookingSeat);
            tvBookingPrice = itemView.findViewById(R.id.tvBookingPrice);
            tvBookingStatus = itemView.findViewById(R.id.tvStatus);
            btnConfirmBooking = itemView.findViewById(R.id.btnConfirmBooking);
            btnCancelBooking = itemView.findViewById(R.id.btnCancelBooking);
        }
    }
}
