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

public class AdminSeatAdapter extends RecyclerView.Adapter<AdminSeatAdapter.SeatViewHolder> {

    private final List<Seat> seatList;
    private final OnSeatClickListener listener;

    public interface OnSeatClickListener {
        void onSeatClick(Seat seat);
    }

    public AdminSeatAdapter(List<Seat> seatList, OnSeatClickListener listener) {
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
        if (seat == null) return;

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

            // Các loại ghế cho Admin:
            // 1. isBooked=true, bookingId!=null → ghế của khách đặt (disable hoàn toàn, xám)
            // 2. isBooked=true, bookingId=null → ghế admin đánh dấu (có thể gỡ, xanh dương nhạt)
            // 3. isBooked=false → ghế trống (có thể chọn)

            boolean isCustomerBooked = seat.isBooked() && seat.getBookingId() != null;
            boolean isAdminMarked = seat.isAdminMarked();

            seatTextView.setEnabled(!isCustomerBooked); // Chỉ disable nếu khách đặt
            seatTextView.setSelected(seat.isSelected());

            int textColor;

            if (isCustomerBooked) {
                // Ghế khách đặt - xám (set background qua state)
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.textSecondary);
                // Background từ drawable sẽ tự xử lý qua state_enabled="false"
            } else if (isAdminMarked) {
                // Ghế admin đánh dấu - xanh dương nhạt, clickable
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.textPrimary);
                // Set background color trực tiếp
                seatTextView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.lightBlue));
            } else if (seat.isSelected()) {
                // Ghế được chọn - trắng
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
            } else {
                // Ghế trống - màu bình thường
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

