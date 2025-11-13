package vn.hcmute.busbooking;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.model.Seat;

public class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.SeatViewHolder> {

    private List<Seat> seatList;
    private OnSeatClickListener listener;

    public interface OnSeatClickListener {
        void onSeatClick(Seat seat);
    }

    public SeatAdapter(List<Seat> seatList) {
        this.seatList = seatList;
    }

    public void setOnSeatClickListener(OnSeatClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seat, parent, false);
        return new SeatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
        Seat seat = seatList.get(position);

        holder.tvSeatNumber.setText(seat.getSeatNumber());

        // Set seat color based on availability and selection
        if (!seat.isAvailable()) {
            holder.itemView.setBackgroundColor(Color.GRAY);
        } else if (seat.isSelected()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFB000"));
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#4CAF50"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (seat.isAvailable() && listener != null) {
                listener.onSeatClick(seat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return seatList != null ? seatList.size() : 0;
    }

    public static class SeatViewHolder extends RecyclerView.ViewHolder {
        TextView tvSeatNumber;

        public SeatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSeatNumber = itemView.findViewById(R.id.tvSeatNumber);
        }
    }
}