package vn.hcmute.busbooking.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Booking;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookingList;
    private OnCancelClickListener cancelClickListener;

    public interface OnCancelClickListener {
        void onCancelClick(int bookingId);
    }

    public BookingAdapter(List<Booking> bookingList, OnCancelClickListener listener) {
        this.bookingList = bookingList;
        this.cancelClickListener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        holder.bind(booking);
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvBookingDate, tvSeatNumber, tvTotalPrice, tvBookingStatus;
        Button btnCancelBooking;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvBookingDate = itemView.findViewById(R.id.tvBookingDate);
            tvSeatNumber = itemView.findViewById(R.id.tvSeatNumber);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvBookingStatus = itemView.findViewById(R.id.tvBookingStatus);
            btnCancelBooking = itemView.findViewById(R.id.btnCancelBooking);
        }

        void bind(Booking booking) {
            String route = booking.getFromLocation() + " → " + booking.getToLocation();
            tvRoute.setText(route);
            tvBookingDate.setText("Ngày đi: " + booking.getDepartureTime());
            tvSeatNumber.setText("Ghế: " + booking.getSeatNumber());
            tvTotalPrice.setText("Giá vé: " + String.format("%,dđ", booking.getTotalPrice()));
            tvBookingStatus.setText(booking.getStatus().toUpperCase());

            // Set status color
            if ("confirmed".equalsIgnoreCase(booking.getStatus())) {
                tvBookingStatus.getBackground().setTint(Color.parseColor("#4CAF50")); // Green
            } else if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
                tvBookingStatus.getBackground().setTint(Color.parseColor("#F44336")); // Red
            } else { // Pending
                tvBookingStatus.getBackground().setTint(Color.parseColor("#FF9800")); // Orange
            }

            // Handle cancel button
            if ("confirmed".equalsIgnoreCase(booking.getStatus())) {
                btnCancelBooking.setVisibility(View.VISIBLE);
                btnCancelBooking.setOnClickListener(v -> {
                    if (cancelClickListener != null) {
                        cancelClickListener.onCancelClick(booking.getId());
                    }
                });
            } else {
                btnCancelBooking.setVisibility(View.GONE);
            }
        }
    }
}
