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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookingViewHolder holder, int position) {
        Map<String, Object> booking = bookings.get(position);

        Object idObj = booking.get("id");
        Object originObj = booking.get("origin");
        Object destObj = booking.get("destination");
        Object seatObj = booking.get("seat_label");
        Object priceObj = booking.get("price_paid");
        Object statusObj = booking.get("status");

        holder.tvBookingId.setText("Đơn #" + (idObj != null ? idObj : "?"));
        holder.tvBookingRoute.setText((originObj != null ? originObj : "?") + " → " + (destObj != null ? destObj : "?"));
        holder.tvBookingSeat.setText(seatObj != null ? seatObj.toString() : "?");
        holder.tvBookingPrice.setText((priceObj != null ? priceObj : "0") + " VNĐ");
        holder.tvBookingStatus.setText(statusObj != null ? statusObj.toString() : "");

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
            tvBookingStatus = itemView.findViewById(R.id.tvBookingStatus);
            btnConfirmBooking = itemView.findViewById(R.id.btnConfirmBooking);
            btnCancelBooking = itemView.findViewById(R.id.btnCancelBooking);
        }
    }
}

