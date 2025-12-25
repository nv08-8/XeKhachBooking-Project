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
        Object seatLabelsObj = booking.get("seat_labels");
        Object priceObj = booking.get("total_amount");
        Object statusObj = booking.get("status");

        if (holder.tvBookingId != null) {
            holder.tvBookingId.setText("Đơn #" + (idObj != null ? idObj.toString() : "?"));
        }
        if (holder.tvBookingRoute != null) {
            holder.tvBookingRoute.setText((originObj != null ? originObj.toString() : "?") + " → " + (destObj != null ? destObj.toString() : "?"));
        }

        String seats = "?";
        if (seatLabelsObj instanceof List) {
            List<?> seatList = (List<?>) seatLabelsObj;
            if (!seatList.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < seatList.size(); i++) {
                    sb.append(seatList.get(i).toString());
                    if (i < seatList.size() - 1) {
                        sb.append(", ");
                    }
                }
                seats = sb.toString();
            }
        } else if (seatLabelsObj != null) {
            seats = seatLabelsObj.toString();
        }
        if (holder.tvBookingSeat != null) {
            holder.tvBookingSeat.setText("Ghế: " + seats);
        }

        String priceString = "0";
        if (priceObj != null) {
            try {
                double price = Double.parseDouble(priceObj.toString());
                priceString = String.format("%,.0f", price);
            } catch (NumberFormatException e) {
                priceString = priceObj.toString();
            }
        }
        if (holder.tvBookingPrice != null) {
            holder.tvBookingPrice.setText(priceString + " VNĐ");
        }

        if (holder.tvBookingStatus != null) {
            holder.tvBookingStatus.setText(statusObj != null ? statusObj.toString() : "");
        }

        if ("pending".equalsIgnoreCase(String.valueOf(statusObj))) {
            if (holder.btnConfirmBooking != null) holder.btnConfirmBooking.setVisibility(View.VISIBLE);
            if (holder.btnCancelBooking != null) holder.btnCancelBooking.setVisibility(View.VISIBLE);
        } else {
            if (holder.btnConfirmBooking != null) holder.btnConfirmBooking.setVisibility(View.GONE);
            if (holder.btnCancelBooking != null) holder.btnCancelBooking.setVisibility(View.GONE);
        }

        if (holder.btnConfirmBooking != null) {
            holder.btnConfirmBooking.setOnClickListener(v -> {
                if (listener != null) listener.onConfirmBooking(booking);
            });
        }

        if (holder.btnCancelBooking != null) {
            holder.btnCancelBooking.setOnClickListener(v -> {
                if (listener != null) listener.onCancelBooking(booking);
            });
        }
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
