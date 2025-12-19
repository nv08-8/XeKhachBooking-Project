package vn.hcmute.busbooking.adapter.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Promotion;

public class PromotionsAdapter extends RecyclerView.Adapter<PromotionsAdapter.PromotionViewHolder> {

    private List<Promotion> promotionList;
    private OnPromotionClickListener listener;

    public interface OnPromotionClickListener {
        void onDeletePromotion(Promotion promotion);
    }

    public PromotionsAdapter(List<Promotion> promotionList, OnPromotionClickListener listener) {
        this.promotionList = promotionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        Promotion promotion = promotionList.get(position);
        holder.tvPromoCode.setText(promotion.getCode());
        holder.tvPromoDetails.setText("Giảm " + promotion.getDiscountValue() + (promotion.getDiscountType().equals("percent") ? "%" : "đ"));
        holder.tvPromoDate.setText("Hết hạn: " + promotion.getEndDate());

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
        TextView tvPromoCode, tvPromoDetails, tvPromoDate;
        ImageButton btnDeletePromotion;

        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPromoCode = itemView.findViewById(R.id.tvPromoCode);
            tvPromoDetails = itemView.findViewById(R.id.tvPromoDetails);
            tvPromoDate = itemView.findViewById(R.id.tvPromoDate);
            btnDeletePromotion = itemView.findViewById(R.id.btnDeletePromotion);
        }
    }
}
