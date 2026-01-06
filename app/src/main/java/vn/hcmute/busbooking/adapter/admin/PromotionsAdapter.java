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

    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return "";
        }
        // Remove timezone info (e.g., "2025-12-31T23:59:59+00:00" -> "2025-12-31 23:59:59")
        try {
            if (dateTime.contains("T")) {
                String cleaned = dateTime.replace("T", " ");
                cleaned = cleaned.replaceAll("[+Z].*$", "");
                cleaned = cleaned.replaceAll("\\.\\d+.*$", "");
                return cleaned;
            }
            return dateTime;
        } catch (Exception e) {
            return dateTime;
        }
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
        // Remove .0 from numbers
        long discountValue = (long) promotion.getDiscount_value();
        holder.tvPromoDetails.setText("Giảm " + discountValue + (promotion.getDiscount_type().equals("percent") ? "%" : "đ"));

        String endDate = promotion.getEnd_date();
        if (endDate != null) {
            // Format datetime to clean format (YYYY-MM-DD HH:mm:ss)
            holder.tvPromoDate.setText("Hết hạn: " + formatDateTime(endDate));
        } else {
            holder.tvPromoDate.setText("Hết hạn: N/A");
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
