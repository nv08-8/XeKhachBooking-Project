package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class RoutesAdapter extends RecyclerView.Adapter<RoutesAdapter.ViewHolder> {

    public interface RouteItemListener {
        void onEdit(int position, Map<String, Object> route);
        void onDelete(int position, Map<String, Object> route);
    }

    private final List<Map<String, Object>> items;
    private final RouteItemListener listener;

    public RoutesAdapter(List<Map<String, Object>> items, RouteItemListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> route = items.get(position);
        Object originObj = route.get("origin");
        Object destinationObj = route.get("destination");
        String origin = originObj != null ? String.valueOf(originObj) : "";
        String destination = destinationObj != null ? String.valueOf(destinationObj) : "";
        Object priceObj = route.get("base_price");
        String price = priceObj != null ? String.valueOf(priceObj) : "";
        String title = origin + " - " + destination;
        String subtitle = "Giá: " + price + " VNĐ";
        holder.tvTitle.setText(title);
        holder.tvSubtitle.setText(subtitle);

        holder.itemView.setOnLongClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add("Sửa").setOnMenuItemClickListener(item -> {
                if (listener != null) listener.onEdit(position, route);
                return true;
            });
            popup.getMenu().add("Xóa").setOnMenuItemClickListener(item -> {
                if (listener != null) listener.onDelete(position, route);
                return true;
            });
            popup.show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvRouteTitle);
            tvSubtitle = itemView.findViewById(R.id.tvRouteSubtitle);
        }
    }
}
