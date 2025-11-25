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
            seatTextView.setText(seat.getLabel());
            seatTextView.setEnabled(!seat.isBooked());
            seatTextView.setSelected(seat.isSelected());

            int textColor;
            if (seat.isBooked()) {
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.textSecondary);
            } else if (seat.isSelected()) {
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
            } else {
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.textPrimary);
            }
            seatTextView.setTextColor(textColor);

            if (!seat.isBooked()) {
                itemView.setOnClickListener(v -> listener.onSeatClick(seat));
            }
        }
    }
}
