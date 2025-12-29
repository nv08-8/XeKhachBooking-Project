package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Seat;

public class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.SeatViewHolder> {

    private final List<Seat> seatList;
    private final OnSeatClickListener listener;

    public interface OnSeatClickListener {
        void onSeatClick(Seat seat);
    }

    public SeatAdapter(List<Seat> seatList, OnSeatClickListener listener) {
        this.seatList = seatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seat, parent, false);
        return new SeatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
        Seat seat = seatList.get(position);
        if (seat == null) return; // Add null check for the seat object itself

        if ("aisle".equals(seat.getSeatType())) {
            holder.itemView.setVisibility(View.INVISIBLE);
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.bind(seat, listener);
        }
    }

    @Override
    public int getItemCount() {
        return seatList != null ? seatList.size() : 0;
    }

    static class SeatViewHolder extends RecyclerView.ViewHolder {
        TextView seatTextView;

        SeatViewHolder(@NonNull View itemView) {
            super(itemView);
            seatTextView = itemView.findViewById(R.id.seatTextView);
        }

        void bind(final Seat seat, final OnSeatClickListener listener) {
            // Set text safely, defaulting to empty string if label is null
            seatTextView.setText(seat.getLabel() != null ? seat.getLabel() : "");

            // Các loại ghế:
            // 1. isBooked=true, bookingId!=null → ghế của khách đặt (disable hoàn toàn)
            // 2. isBooked=true, bookingId=null → ghế admin đánh dấu (có thể gỡ)
            // 3. isBooked=false → ghế trống (có thể chọn)

            boolean isCustomerBooked = seat.isBooked() && seat.getBookingId() != null;
            boolean isAdminMarked = seat.isAdminMarked();

            seatTextView.setEnabled(!isCustomerBooked); // Chỉ disable nếu khách đặt
            seatTextView.setSelected(seat.isSelected());

            int textColor;
            if (isCustomerBooked) {
                // Ghế khách đặt - xám, không thể tương tác
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.textSecondary);
            } else if (seat.isSelected()) {
                // Ghế được chọn - trắng
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
            } else {
                // Ghế trống hoặc admin đánh dấu - màu bình thường
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.textPrimary);
            }
            seatTextView.setTextColor(textColor);

            if (isCustomerBooked) {
                // Ghế khách đặt - không cho click
                itemView.setOnClickListener(null);
            } else {
                // Ghế trống hoặc admin đánh dấu - cho click
                itemView.setOnClickListener(v -> listener.onSeatClick(seat));
            }
        }
    }
}
