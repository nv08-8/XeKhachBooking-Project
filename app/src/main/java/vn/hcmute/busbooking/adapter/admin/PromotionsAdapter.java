package vn.hcmute.busbooking.adapter.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Promotion;

public class PromotionsAdapter extends RecyclerView.Adapter<PromotionsAdapter.PromotionViewHolder> {

    private List<Promotion> promotionList;
    private OnPromotionClickListener listener;

    public interface OnPromotionClickListener {
        void onEditPromotion(Promotion promotion);
        void onDeletePromotion(Promotion promotion);
    }

    public PromotionsAdapter(List<Promotion> promotionList, OnPromotionClickListener listener) {
        this.promotionList = promotionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        Promotion promotion = promotionList.get(position);
        holder.chipPromoCode.setText(promotion.getCode());
        holder.tvPromoDetails.setText("Giảm " + promotion.getDiscount_value() + (promotion.getDiscount_type().equals("percent") ? "%" : "đ"));
        
        String endDate = promotion.getEnd_date();
        if (endDate != null && endDate.contains("T")) {
            String formattedDate = endDate.substring(0, endDate.indexOf("T"));
            holder.tvPromoDate.setText("Hết hạn: " + formattedDate);
        } else {
            holder.tvPromoDate.setText("Hết hạn: " + endDate);
        }

        holder.btnEditPromotion.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditPromotion(promotion);
            }
        });

        holder.btnDeletePromotion.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeletePromotion(promotion);
            }
        });
    }

    @Override
    public int getItemCount() {
        return promotionList.size();
    }

    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        Chip chipPromoCode;
        TextView tvPromoDetails, tvPromoDate;
        ImageButton btnEditPromotion, btnDeletePromotion;

        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            chipPromoCode = itemView.findViewById(R.id.chipPromoCode);
            tvPromoDetails = itemView.findViewById(R.id.tvPromoDetails);
            tvPromoDate = itemView.findViewById(R.id.tvPromoDate);
            btnEditPromotion = itemView.findViewById(R.id.btnEditPromotion);
            btnDeletePromotion = itemView.findViewById(R.id.btnDeletePromotion);
        }
    }
}
