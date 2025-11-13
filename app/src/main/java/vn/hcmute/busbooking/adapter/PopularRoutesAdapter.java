package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
        holder.ivRouteImage.setImageResource(route.getImageResource());
    }

    @Override
    public int getItemCount() {
        return routeList.size();
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
