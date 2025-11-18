package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Trip;

// PHIÊN BẢN ĐÃ SỬA LẠI HOÀN CHỈNH
public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> tripList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Trip trip);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public TripAdapter(List<Trip> tripList) {
        this.tripList = tripList;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.bind(trip, listener);
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public void updateTrips(List<Trip> newTrips) {
        tripList.clear();
        tripList.addAll(newTrips);
        notifyDataSetChanged();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        // KHAI BÁO ĐÚNG CÁC VIEW THEO item_trip.xml
        TextView txtRoute, txtDeparture, txtPrice, txtAvailableSeats;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            // ÁNH XẠ ĐÚNG CÁC ID TỪ LAYOUT
            txtRoute = itemView.findViewById(R.id.txtRoute);
            txtDeparture = itemView.findViewById(R.id.txtDeparture);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtAvailableSeats = itemView.findViewById(R.id.txtAvailableSeats);
        }

        public void bind(final Trip trip, final OnItemClickListener listener) {
            String route = trip.getFromLocation() + " - " + trip.getToLocation();
            txtRoute.setText(route);
            txtDeparture.setText("Giờ đi: " + trip.getDepartureTime());
            txtPrice.setText(String.format("%,d đ", trip.getPrice()));
            txtAvailableSeats.setText("Loại xe: " + trip.getBusType());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(trip);
                }
            });
        }
    }
}
