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
import java.util.ArrayList;
import java.util.List;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.PopularRoute;

public class PopularRoutesAdapter extends RecyclerView.Adapter<PopularRoutesAdapter.ViewHolder> {

    private List<PopularRoute> routeList;

    public PopularRoutesAdapter(List<PopularRoute> routeList) {
        this.routeList = routeList;
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
        // Load image asynchronously with Glide (handles decoding off the UI thread)
        Glide.with(holder.ivRouteImage.getContext())
                .load(route.getImageResource())
                .centerCrop()
                .into(holder.ivRouteImage);
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
