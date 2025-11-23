package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Trip;

public class TripsAdapter extends RecyclerView.Adapter<TripsAdapter.TripViewHolder> {

    private List<Trip> tripList;
    private OnTripClickListener listener;

    public interface OnTripClickListener {
        void onEditTrip(Trip trip);
        void onDeleteTrip(Trip trip);
    }

    public TripsAdapter(List<Trip> tripList, OnTripClickListener listener) {
        this.tripList = tripList;
        this.listener = listener;
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
        holder.tvTripName.setText(trip.getOrigin() + " - " + trip.getDestination());
        holder.tvTripDetails.setText(trip.getOperator() + " - " + trip.getBusType());

        holder.btnEditTrip.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditTrip(trip);
            }
        });

        holder.btnDeleteTrip.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteTrip(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTripName, tvTripDetails;
        ImageButton btnEditTrip, btnDeleteTrip;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripName = itemView.findViewById(R.id.tvTripName);
            tvTripDetails = itemView.findViewById(R.id.tvTripDetails);
            btnEditTrip = itemView.findViewById(R.id.btnEditTrip);
            btnDeleteTrip = itemView.findViewById(R.id.btnDeleteTrip);
        }
    }
}
