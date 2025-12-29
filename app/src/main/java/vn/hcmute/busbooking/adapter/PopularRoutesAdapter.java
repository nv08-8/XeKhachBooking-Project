package vn.hcmute.busbooking.adapter;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import java.util.ArrayList;
import java.util.List;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.PopularRoute;

public class PopularRoutesAdapter extends RecyclerView.Adapter<PopularRoutesAdapter.ViewHolder> {

    private List<PopularRoute> routeList;
    private OnRouteClickListener clickListener;

    public interface OnRouteClickListener {
        void onRouteClick(String origin, String destination);
    }

    public PopularRoutesAdapter(List<PopularRoute> routeList) {
        this.routeList = routeList;
    }

    public PopularRoutesAdapter(List<PopularRoute> routeList, OnRouteClickListener listener) {
        this.routeList = routeList;
        this.clickListener = listener;
    }

    public void setOnRouteClickListener(OnRouteClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popular_route, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PopularRoute route = routeList.get(position);
        holder.tvRouteName.setText(route.getName());
        holder.tvRoutePrice.setText(route.getPrice());

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                // Parse route name to extract origin and destination
                // Format: "Đà Lạt → Nha Trang" or "Đà Lạt - Nha Trang"
                String routeName = route.getName();
                String[] parts = routeName.split("→|->|−|—");

                if (parts.length >= 2) {
                    String origin = parts[0].trim();
                    String destination = parts[1].trim();
                    android.util.Log.d("PopularRoutesAdapter", "Route clicked: " + origin + " -> " + destination);
                    clickListener.onRouteClick(origin, destination);
                } else {
                    android.util.Log.w("PopularRoutesAdapter", "Could not parse route name: " + routeName);
                }
            }
        });

        // Load image from URL if available, otherwise use drawable resource
        if (route.hasImageUrl()) {
            android.util.Log.d("PopularRoutesAdapter", "Loading image for: " + route.getName() + " from URL: " + route.getImageUrl());
            Glide.with(holder.ivRouteImage.getContext())
                    .load(route.getImageUrl())
                    .override(600, 350)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .disallowHardwareConfig()
                    .centerCrop()
                    .placeholder(R.drawable.img_route1) // Show placeholder while loading
                    .error(R.drawable.img_route1) // Show fallback if URL fails
                    .timeout(10000) // 10 second timeout
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("PopularRoutesAdapter", "Failed to load image: " + route.getImageUrl(), e);
                            return false; // Return false to allow error placeholder to be shown
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("PopularRoutesAdapter", "Successfully loaded image for: " + route.getName());
                            return false; // Return false to allow Glide to handle the resource
                        }
                    })
                    .into(holder.ivRouteImage);
        } else {
            android.util.Log.d("PopularRoutesAdapter", "Using fallback drawable for: " + route.getName());
            holder.ivRouteImage.setImageResource(route.getImageResource());
        }
    }

    @Override
    public int getItemCount() {
        return routeList == null ? 0 : routeList.size();
    }

    public void updateData(List<PopularRoute> newList) {
        if (this.routeList == null) {
            this.routeList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
            notifyDataSetChanged();
            return;
        }

        final List<PopularRoute> oldList = new ArrayList<>(this.routeList);
        final List<PopularRoute> updated = newList != null ? new ArrayList<>(newList) : new ArrayList<>();

        // Compute diff on background thread
        new Thread(() -> {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() { return oldList.size(); }
                @Override
                public int getNewListSize() { return updated.size(); }
                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    String oldName = oldList.get(oldItemPosition).getName();
                    String newName = updated.get(newItemPosition).getName();
                    return oldName != null && oldName.equals(newName);
                }
                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    PopularRoute o = oldList.get(oldItemPosition);
                    PopularRoute n = updated.get(newItemPosition);
                    return (o.getName() == null ? n.getName() == null : o.getName().equals(n.getName()))
                            && (o.getPrice() == null ? n.getPrice() == null : o.getPrice().equals(n.getPrice()));
                }
            });

            // Apply updates on main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                routeList.clear();
                routeList.addAll(updated);
                diffResult.dispatchUpdatesTo(PopularRoutesAdapter.this);
            });
        }).start();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRouteImage;
        TextView tvRouteName;
        TextView tvRoutePrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRouteImage = itemView.findViewById(R.id.ivRouteImage);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            tvRoutePrice = itemView.findViewById(R.id.tvRoutePrice);
        }
    }
}
