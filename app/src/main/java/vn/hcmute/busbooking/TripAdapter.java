package vn.hcmute.busbooking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import vn.hcmute.busbooking.model.Trip;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> tripList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Trip trip);
    }

    public TripAdapter(List<Trip> tripList) {
        this.tripList = tripList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateTrips(List<Trip> newTrips) {
        this.tripList = newTrips;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.txtRoute.setText(trip.getFromLocation() + " → " + trip.getToLocation());
        holder.txtDeparture.setText("Khởi hành: " + trip.getDepartureTime() + " | Đến: " + trip.getArrivalTime());
        holder.txtPrice.setText(String.format("Giá: %d ₫ | %s", trip.getPrice(), trip.getBusType()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tripList != null ? tripList.size() : 0;
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView txtRoute, txtDeparture, txtPrice;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRoute = itemView.findViewById(R.id.txtRoute);
            txtDeparture = itemView.findViewById(R.id.txtDeparture);
            txtPrice = itemView.findViewById(R.id.txtPrice);
        }
    }
}
