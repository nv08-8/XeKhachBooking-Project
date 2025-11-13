package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Promotion;

public class PromotionsAdapter extends RecyclerView.Adapter<PromotionsAdapter.ViewHolder> {

    private List<Promotion> promotionList;

    public PromotionsAdapter(List<Promotion> promotionList) {
        this.promotionList = promotionList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promotion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Promotion promotion = promotionList.get(position);
        holder.ivPromotionImage.setImageResource(promotion.getImageResource());
    }

    @Override
    public int getItemCount() {
        return promotionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPromotionImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPromotionImage = itemView.findViewById(R.id.ivPromotionImage);
        }
    }
}
