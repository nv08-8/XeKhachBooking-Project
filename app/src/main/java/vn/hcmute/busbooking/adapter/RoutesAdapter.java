package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class RoutesAdapter extends RecyclerView.Adapter<RoutesAdapter.RouteViewHolder> {

    private List<Map<String, Object>> routes;
    private OnRouteClickListener listener;

    public interface OnRouteClickListener {
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

        String title = (originObj != null ? originObj : "?") + " - " + (destObj != null ? destObj : "?");
        holder.tvRouteTitle.setText(title);

        if (priceObj != null) {
            holder.tvRoutePrice.setText("Giá: " + priceObj + " VNĐ");
        }

        if (durationObj != null) {
            holder.tvRouteDuration.setText(durationObj + " phút");
        }

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
        TextView tvRouteTitle, tvRoutePrice, tvRouteDuration;
        Button btnEditRoute, btnDeleteRoute;

        public RouteViewHolder(View itemView) {
            super(itemView);
            tvRouteTitle = itemView.findViewById(R.id.tvRouteTitle);
            tvRoutePrice = itemView.findViewById(R.id.tvRoutePrice);
            tvRouteDuration = itemView.findViewById(R.id.tvRouteDuration);
            btnEditRoute = itemView.findViewById(R.id.btnEditRoute);
            btnDeleteRoute = itemView.findViewById(R.id.btnDeleteRoute);
        }
    }
}

