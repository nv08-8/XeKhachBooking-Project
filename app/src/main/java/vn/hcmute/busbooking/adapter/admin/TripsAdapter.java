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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
        void onManageSeats(Map<String, Object> trip);
    }

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

        holder.tvOperator.setText(String.valueOf(trip.get("operator")));
        holder.tvVehicleType.setText(String.valueOf(trip.get("bus_type")));
        holder.tvOrigin.setText(String.valueOf(trip.get("origin")));
        holder.tvDestination.setText(String.valueOf(trip.get("destination")));

        try {
             holder.tvPrice.setText(String.format(Locale.GERMANY, "%,.0fđ", Double.parseDouble(String.valueOf(trip.get("price")))));
        } catch(Exception e) {
            holder.tvPrice.setText(trip.get("price") + "đ");
        }

        String departureTimeStr = String.valueOf(trip.get("departure_time"));
        String arrivalTimeStr = String.valueOf(trip.get("arrival_time"));

        // Format dates beautifully
        Date departureDate = parseDate(departureTimeStr);
        Date arrivalDate = parseDate(arrivalTimeStr);

        SimpleDateFormat sdfOutTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat sdfOutDate = new SimpleDateFormat("EEE, dd MMM yyyy", new Locale("vi", "VN"));

        if (departureDate != null) {
            holder.tvDepartureTime.setText(sdfOutTime.format(departureDate));
            holder.tvDate.setText(sdfOutDate.format(departureDate));
        } else {
            // Fallback to substring if parsing fails
            if (departureTimeStr != null && departureTimeStr.length() >= 16) {
                holder.tvDepartureTime.setText(departureTimeStr.substring(11, 16));
            } else {
                holder.tvDepartureTime.setText(departureTimeStr);
            }
            holder.tvDate.setText("");
        }

        if (arrivalDate != null) {
            holder.tvArrivalTime.setText(sdfOutTime.format(arrivalDate));
        } else {
            if (arrivalTimeStr != null && arrivalTimeStr.length() >= 16) {
                holder.tvArrivalTime.setText(arrivalTimeStr.substring(11, 16));
            } else {
                holder.tvArrivalTime.setText(arrivalTimeStr);
            }
        }

        // Calculate and set duration
        if (departureDate != null && arrivalDate != null) {
            long diff = arrivalDate.getTime() - departureDate.getTime();
            long hours = diff / (1000 * 60 * 60);
            long minutes = (diff / (1000 * 60)) % 60;
            if (hours > 0) {
                holder.tvDuration.setText(String.format(Locale.getDefault(), "%d giờ %02d phút", hours, minutes));
            } else {
                holder.tvDuration.setText(String.format(Locale.getDefault(), "%d phút", minutes));
            }
        } else {
            holder.tvDuration.setText("");
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

        holder.btnSeats.setOnClickListener(v -> {
            if (listener != null) {
                listener.onManageSeats(trip);
            }
        });

        if (sessionManager.isAdmin()) {
            holder.adminActionsLayout.setVisibility(View.VISIBLE);
        } else {
            holder.adminActionsLayout.setVisibility(View.GONE);
        }
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("null")) return null;
        
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        };
        
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                return sdf.parse(dateStr);
            } catch (ParseException ignored) {}
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return tripList != null ? tripList.size() : 0;
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvOperator, tvVehicleType, tvPrice, tvDepartureTime, tvOrigin, tvArrivalTime, tvDestination, tvDuration, tvDate;
        LinearLayout adminActionsLayout;
        Button btnEdit, btnDelete, btnSeats;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOperator = itemView.findViewById(R.id.tvOperator);
            tvVehicleType = itemView.findViewById(R.id.tvVehicleType);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvArrivalTime = itemView.findViewById(R.id.tvArrivalTime);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDate = itemView.findViewById(R.id.tvDate);
            adminActionsLayout = itemView.findViewById(R.id.adminActionsLayout);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnSeats = itemView.findViewById(R.id.btnSeats);
        }
    }
}
