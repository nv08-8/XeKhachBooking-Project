package vn.hcmute.busbooking.adapter.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.utils.SessionManager;

public class TripsAdapter extends RecyclerView.Adapter<TripsAdapter.TripViewHolder> {

    private List<Map<String, Object>> tripList;
    private OnTripClickListener listener;
    private SessionManager sessionManager;

    public interface OnTripClickListener {
        void onEditTrip(Map<String, Object> trip);
        void onDeleteTrip(Map<String, Object> trip);
    }

    // Correct constructor with 3 parameters
    public TripsAdapter(List<Map<String, Object>> tripList, OnTripClickListener listener, Context context) {
        this.tripList = tripList;
        this.listener = listener;
        this.sessionManager = new SessionManager(context);
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Map<String, Object> trip = tripList.get(position);

        // Set text for the views
        holder.tvRoute.setText(trip.get("origin") + " → " + trip.get("destination"));
        holder.tvOperatorAndBusType.setText(trip.get("operator") + " - " + trip.get("bus_type"));
        holder.tvDepartureTime.setText("Đi: " + trip.get("departure_time")); // You might want to format this date
        holder.tvPrice.setText(trip.get("price") + " VNĐ");

        // Only show admin actions if the user is an admin
        if (sessionManager.isAdmin()) {
            holder.adminActionsLayout.setVisibility(View.VISIBLE);
        } else {
            holder.adminActionsLayout.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditTrip(trip);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteTrip(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tripList != null ? tripList.size() : 0;
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvOperatorAndBusType, tvDepartureTime, tvPrice;
        LinearLayout adminActionsLayout;
        Button btnEdit, btnDelete;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvOperatorAndBusType = itemView.findViewById(R.id.tvOperatorAndBusType);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            adminActionsLayout = itemView.findViewById(R.id.adminActionsLayout);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
