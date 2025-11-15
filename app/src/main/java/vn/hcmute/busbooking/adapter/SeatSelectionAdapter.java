package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Seat;

public class SeatSelectionAdapter extends RecyclerView.Adapter<SeatSelectionAdapter.SeatViewHolder> {

    private List<Seat> seatList;
    private OnSeatClickListener listener;
    private Set<String> selectedSeats = new HashSet<>();

    public interface OnSeatClickListener {
        void onSeatClick(Seat seat);
    }

    public SeatSelectionAdapter(List<Seat> seatList, OnSeatClickListener listener) {
        this.seatList = seatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seat_selection, parent, false);
        return new SeatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
        Seat seat = seatList.get(position);
        holder.tvSeatLabel.setText(seat.getLabel());

        boolean isSelected = selectedSeats.contains(seat.getLabel());

        if (!seat.isAvailable()) {
            // Seat is booked
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray)
            );
            holder.tvSeatLabel.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white)
            );
            holder.itemView.setEnabled(false);
        } else if (isSelected) {
            // Seat is selected
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary)
            );
            holder.tvSeatLabel.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white)
            );
            holder.itemView.setEnabled(true);
        } else {
            // Seat is available
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white)
            );
            holder.tvSeatLabel.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black)
            );
            holder.itemView.setEnabled(true);
        }

        holder.itemView.setOnClickListener(v -> {
            if (seat.isAvailable()) {
                if (selectedSeats.contains(seat.getLabel())) {
                    selectedSeats.remove(seat.getLabel());
                } else {
                    selectedSeats.add(seat.getLabel());
                }
                notifyItemChanged(position);
                if (listener != null) {
                    listener.onSeatClick(seat);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return seatList.size();
    }

    static class SeatViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvSeatLabel;

        public SeatViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardViewSeat);
            tvSeatLabel = itemView.findViewById(R.id.tvSeatLabel);
        }
    }
}

