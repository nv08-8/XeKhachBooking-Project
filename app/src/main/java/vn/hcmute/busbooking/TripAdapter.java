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

        holder.tvBusOperator.setText(trip.getOperator());
        holder.tvBusType.setText(trip.getBusType());
        holder.tvPrice.setText(String.format("%,.0fđ", trip.getPrice()));

        String departureTime = trip.getDepartureTime();
        if (departureTime != null && departureTime.length() > 16) {
            String time = departureTime.substring(11, 16);
            holder.tvDepartureTime.setText(time);
        } else {
            holder.tvDepartureTime.setText("N/A");
        }

        String arrivalTime = trip.getArrivalTime();
        if (arrivalTime != null && arrivalTime.length() > 16) {
            String time = arrivalTime.substring(11, 16);
            holder.tvArrivalTime.setText(time);
        } else {
            holder.tvArrivalTime.setText("N/A");
        }

        holder.tvDepartureStation.setText(trip.getFromLocation());
        holder.tvArrivalStation.setText(trip.getToLocation());

        Double duration = trip.getDurationHours();
        Integer seats = trip.getSeatsAvailable();

        String durationStr = "";
        if (duration != null) {
            if (duration % 1 == 0) {
                durationStr = String.format("%.0f", duration) + " hours";
            } else {
                durationStr = String.format("%.1f", duration) + " hours";
            }
        }

        String seatsStr = "";
        if (seats != null) {
            seatsStr = seats + " seats left";
        }

        String tripInfo = "";
        if (!durationStr.isEmpty()) {
            tripInfo += durationStr;
        }
        if (!seatsStr.isEmpty()) {
            if (!tripInfo.isEmpty()) {
                tripInfo += " • ";
            }
            tripInfo += seatsStr;
        }
        holder.tvTripInfo.setText(tripInfo);


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
        TextView tvBusOperator, tvBusType, tvPrice, tvDepartureTime, tvArrivalTime,
                tvDepartureStation, tvArrivalStation, tvTripInfo;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusOperator = itemView.findViewById(R.id.tvBusOperator);
            tvBusType = itemView.findViewById(R.id.tvBusType);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvArrivalTime = itemView.findViewById(R.id.tvArrivalTime);
            tvDepartureStation = itemView.findViewById(R.id.tvDepartureStation);
            tvArrivalStation = itemView.findViewById(R.id.tvArrivalStation);
            tvTripInfo = itemView.findViewById(R.id.tvTripInfo);
        }
    }
}
