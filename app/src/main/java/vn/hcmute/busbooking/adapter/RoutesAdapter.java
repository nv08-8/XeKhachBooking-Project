package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class RoutesAdapter extends RecyclerView.Adapter<RoutesAdapter.RouteViewHolder> {

    private List<Map<String, Object>> routes;
    private OnRouteClickListener listener;

    public interface OnRouteClickListener {
        void onRouteClick(Map<String, Object> route);
        void onEditRoute(Map<String, Object> route);
        void onDeleteRoute(Map<String, Object> route);
    }

    public RoutesAdapter(List<Map<String, Object>> routes, OnRouteClickListener listener) {
        this.routes = routes;
        this.listener = listener;
    }

    @Override
    public RouteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RouteViewHolder holder, int position) {
        Map<String, Object> route = routes.get(position);

        Object originObj = route.get("origin");
        Object destObj = route.get("destination");
        Object priceObj = route.get("price");
        Object durationObj = route.get("duration_min");
        Object distanceObj = route.get("distance_km");

        String title = (originObj != null ? originObj : "?") + " - " + (destObj != null ? destObj : "?");
        holder.tvRouteTitle.setText(title);

        if (priceObj != null && holder.tvRoutePrice != null) {
            holder.tvRoutePrice.setText("Giá: " + priceObj + " VNĐ");
        }

        if (durationObj != null) {
            holder.tvRouteDuration.setText(durationObj + " phút");
        }

        if (holder.tvRouteTripCount != null) {
            // Attempt to get trip count from multiple possible keys
            Object tripCountObj = route.get("upcoming_trip_count");
            if (tripCountObj == null) {
                tripCountObj = route.get("trips_count");
            }

            int tripCount = 0; // Default to 0
            if (tripCountObj != null) {
                try {
                    // Use Double.parseDouble to handle potential float values (e.g., 10.0)
                    tripCount = (int) Double.parseDouble(tripCountObj.toString());
                } catch (NumberFormatException e) {
                    tripCount = 0; // If parsing fails, default to 0
                }
            }
            holder.tvRouteTripCount.setText("Sắp khởi hành: " + tripCount);
        }


        if (distanceObj != null && holder.tvRouteDistance != null) {
            try {
                double distance = Double.parseDouble(distanceObj.toString());
                if (distance == (long) distance) {
                    holder.tvRouteDistance.setText(String.format("%d km", (long) distance));
                } else {
                    holder.tvRouteDistance.setText(String.format("%.1f km", distance));
                }
            } catch (NumberFormatException e) {
                holder.tvRouteDistance.setText("");
            }
        } else if (holder.tvRouteDistance != null) {
            holder.tvRouteDistance.setText("");
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRouteClick(route);
            }
        });

        holder.btnEditRoute.setOnClickListener(v -> {
            if (listener != null) listener.onEditRoute(route);
        });

        holder.btnDeleteRoute.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteRoute(route);
        });
    }

    @Override
    public int getItemCount() {
        return routes != null ? routes.size() : 0;
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView tvRouteTitle, tvRoutePrice, tvRouteDuration, tvRouteTripCount, tvRouteDistance;
        ImageView btnEditRoute, btnDeleteRoute;

        public RouteViewHolder(View itemView) {
            super(itemView);
            tvRouteTitle = itemView.findViewById(R.id.tvRouteTitle);
            tvRoutePrice = itemView.findViewById(R.id.tvRoutePrice);
            tvRouteDuration = itemView.findViewById(R.id.tvRouteDuration);
            tvRouteTripCount = itemView.findViewById(R.id.tvRouteTripCount);
            tvRouteDistance = itemView.findViewById(R.id.tvRouteDistance);
            btnEditRoute = itemView.findViewById(R.id.btnEditRoute);
            btnDeleteRoute = itemView.findViewById(R.id.btnDeleteRoute);
        }
    }
}
