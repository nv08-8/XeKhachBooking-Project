package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class TripFeedbackAdapter extends RecyclerView.Adapter<TripFeedbackAdapter.TripViewHolder> {

    private List<Map<String, Object>> tripList;
    private OnTripClickListener listener;

    public interface OnTripClickListener {
        void onTripClick(int tripId);
    }

    public TripFeedbackAdapter(List<Map<String, Object>> tripList, OnTripClickListener listener) {
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_feedback_list, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Map<String, Object> trip = tripList.get(position);

        // Lấy thông tin trip
        String origin = (trip.get("origin") != null) ? trip.get("origin").toString() : "";
        String destination = (trip.get("destination") != null) ? trip.get("destination").toString() : "";
        String departureTimeRaw = (trip.get("departure_time") != null) ? trip.get("departure_time").toString() : "";
        String operator = (trip.get("operator") != null) ? trip.get("operator").toString() : "";
        String busType = (trip.get("bus_type") != null) ? trip.get("bus_type").toString() : "";
        Object feedbackCountObj = trip.get("feedback_count");
        int feedbackCount = (feedbackCountObj != null) ? Integer.parseInt(feedbackCountObj.toString()) : 0;

        // Format thời gian
        String formattedTime = formatDateTime(departureTimeRaw);

        holder.tvRoute.setText(origin + " → " + destination);
        holder.tvDeparture.setText(formattedTime);
        holder.tvOperator.setText(operator);
        holder.tvBusType.setText(busType);
        holder.tvFeedbackCount.setText(String.valueOf(feedbackCount));

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            Object tripIdObj = trip.get("id");
            if (tripIdObj != null && listener != null) {
                int tripId = Integer.parseInt(tripIdObj.toString());
                listener.onTripClick(tripId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    // ✅ Format ISO timestamp thành format đẹp: HH:mm dd/MM/yyyy
    private String formatDateTime(String isoDateTime) {
        try {
            // Parse ISO format: 2026-01-04T16:00:00.000Z
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            Date date = isoFormat.parse(isoDateTime);

            // Format thành: HH:mm dd/MM/yyyy (Ví dụ: 16:00 04/01/2026)
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.US);
            return outputFormat.format(date);
        } catch (ParseException e) {
            // Nếu parse lỗi, trả về thời gian gốc
            return isoDateTime;
        }
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvDeparture, tvOperator, tvBusType, tvFeedbackCount;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvDeparture = itemView.findViewById(R.id.tvDeparture);
            tvOperator = itemView.findViewById(R.id.tvOperator);
            tvBusType = itemView.findViewById(R.id.tvBusType);
            tvFeedbackCount = itemView.findViewById(R.id.tvFeedbackCount);
        }
    }
}

