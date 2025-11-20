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

        // LOGIC UPDATE: Use isBooked() if available, or !isAvailable()
        // Assuming your Seat model has isBooked() mapped to the API's "is_booked"
        if (seat.isBooked()) {
            // --- CASE 1: BOOKED SEAT (GREY) ---
            holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray)
            );
            holder.tvSeatLabel.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white)
            );

            holder.itemView.setEnabled(false);
            holder.itemView.setOnClickListener(null); // CRITICAL: Remove click listener completely

        } else if (isSelected) {
            // --- CASE 2: SELECTED SEAT (ORANGE/PRIMARY) ---
            holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary)
            );
            holder.tvSeatLabel.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white)
            );

            holder.itemView.setEnabled(true);
            setClickListener(holder, seat, position); // Helper method to attach listener

        } else {
            // --- CASE 3: AVAILABLE SEAT (WHITE) ---
            holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white)
            );
            holder.tvSeatLabel.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black)
            );

            holder.itemView.setEnabled(true);
            setClickListener(holder, seat, position); // Helper method to attach listener
        }
    }

    // Helper method to avoid code duplication
    private void setClickListener(SeatViewHolder holder, Seat seat, int position) {
        holder.itemView.setOnClickListener(v -> {
            // Double check availability just in case
            if (!seat.isBooked()) {
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

