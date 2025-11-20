package vn.hcmute.busbooking.adapter;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
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
        Glide.with(holder.ivPromotionImage.getContext())
                .load(promotion.getImageResource())
                .centerCrop()
                .into(holder.ivPromotionImage);
    }

    @Override
    public int getItemCount() {
        return promotionList == null ? 0 : promotionList.size();
    }

    public void updateData(List<Promotion> newList) {
        if (this.promotionList == null) {
            this.promotionList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
            notifyDataSetChanged();
            return;
        }

        final List<Promotion> oldList = new ArrayList<>(this.promotionList);
        final List<Promotion> updated = newList != null ? new ArrayList<>(newList) : new ArrayList<>();

        new Thread(() -> {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override public int getOldListSize() { return oldList.size(); }
                @Override public int getNewListSize() { return updated.size(); }
                @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    Promotion o = oldList.get(oldItemPosition);
                    Promotion n = updated.get(newItemPosition);
                    return o.getTitle() != null && o.getTitle().equals(n.getTitle());
                }
                @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Promotion o = oldList.get(oldItemPosition);
                    Promotion n = updated.get(newItemPosition);
                    return (o.getTitle() == null ? n.getTitle() == null : o.getTitle().equals(n.getTitle()))
                            && (o.getDescription() == null ? n.getDescription() == null : o.getDescription().equals(n.getDescription()));
                }
            });

            new Handler(Looper.getMainLooper()).post(() -> {
                promotionList.clear();
                promotionList.addAll(updated);
                diffResult.dispatchUpdatesTo(PromotionsAdapter.this);
            });
        }).start();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPromotionImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPromotionImage = itemView.findViewById(R.id.ivPromotionImage);
        }
    }
}
