package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.FaqItem;

public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FaqViewHolder> {

    private final List<FaqItem> faqItems;

    public FaqAdapter(List<FaqItem> faqItems) {
        this.faqItems = faqItems;
    }

    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faq, parent, false);
        return new FaqViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {
        FaqItem faqItem = faqItems.get(position);
        holder.tvQuestion.setText(faqItem.getQuestion());
        holder.tvAnswer.setText(faqItem.getAnswer());

        boolean isExpanded = faqItem.isExpanded();
        holder.tvAnswer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            faqItem.setExpanded(!isExpanded);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return faqItems.size();
    }

    static class FaqViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion;
        TextView tvAnswer;

        public FaqViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
        }
    }
}
