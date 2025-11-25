package vn.hcmute.busbooking.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Location;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private final List<Location> locationList;
    private final Context context;
    private int selectedPosition = -1; // -1 for no selection
    private final OnLocationClickListener listener;

    public interface OnLocationClickListener {
        void onLocationClick(Location location);
    }

    public LocationAdapter(Context context, List<Location> locationList, OnLocationClickListener listener) {
        this.context = context;
        this.locationList = locationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locationList.get(position);
        holder.tvLocationName.setText(location.getName());
        holder.tvLocationAddress.setText(location.getAddress());

        // Update UI based on selection state
        if (position == selectedPosition) {
            ((MaterialCardView) holder.itemView).setStrokeColor(ContextCompat.getColor(context, R.color.colorPrimary));
            ((MaterialCardView) holder.itemView).setStrokeWidth(4);
        } else {
            ((MaterialCardView) holder.itemView).setStrokeColor(Color.parseColor("#E0E0E0"));
            ((MaterialCardView) holder.itemView).setStrokeWidth(2);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            // use getBindingAdapterPosition to avoid deprecated getAdapterPosition warnings
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;
            selectedPosition = adapterPos;
            // Notify change for both old and new selected items to update their UI
            if (previousPosition != -1) notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            listener.onLocationClick(location);
        });
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    // New: allow setting selected location programmatically
    public void setSelectedLocation(Location location) {
        if (location == null) {
            int prev = selectedPosition;
            selectedPosition = -1;
            if (prev != -1) notifyItemChanged(prev);
            return;
        }
        for (int i = 0; i < locationList.size(); i++) {
            Location l = locationList.get(i);
            if (l != null && l.getId() == location.getId()) {
                int prev = selectedPosition;
                selectedPosition = i;
                if (prev != -1) notifyItemChanged(prev);
                notifyItemChanged(selectedPosition);
                return;
            }
        }
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName, tvLocationAddress;
        ImageView ivLocationIcon;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
            tvLocationAddress = itemView.findViewById(R.id.tvLocationAddress);
            ivLocationIcon = itemView.findViewById(R.id.ivLocationIcon);
        }
    }
}
