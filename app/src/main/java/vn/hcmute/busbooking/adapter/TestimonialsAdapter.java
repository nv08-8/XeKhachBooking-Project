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
import vn.hcmute.busbooking.model.Testimonial;

public class TestimonialsAdapter extends RecyclerView.Adapter<TestimonialsAdapter.ViewHolder> {

    private List<Testimonial> testimonialList;

    public TestimonialsAdapter(List<Testimonial> testimonialList) {
        this.testimonialList = testimonialList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_testimonial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Testimonial testimonial = testimonialList.get(position);
        holder.tvCustomerName.setText(testimonial.getCustomerName());
        holder.tvCustomerTitle.setText(testimonial.getCustomerTitle());
        holder.tvTestimonialText.setText(testimonial.getTestimonialText());
        holder.ratingBar.setRating(testimonial.getRating());
        Glide.with(holder.ivCustomerImage.getContext())
                .load(testimonial.getCustomerImage())
                .circleCrop()
                .into(holder.ivCustomerImage);
    }

    @Override
    public int getItemCount() {
        return testimonialList == null ? 0 : testimonialList.size();
    }

    public void updateData(List<Testimonial> newList) {
        if (this.testimonialList == null) {
            this.testimonialList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
            notifyDataSetChanged();
            return;
        }

        final List<Testimonial> oldList = new ArrayList<>(this.testimonialList);
        final List<Testimonial> updated = newList != null ? new ArrayList<>(newList) : new ArrayList<>();

        new Thread(() -> {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override public int getOldListSize() { return oldList.size(); }
                @Override public int getNewListSize() { return updated.size(); }
                @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    Testimonial o = oldList.get(oldItemPosition);
                    Testimonial n = updated.get(newItemPosition);
                    return o.getCustomerName() != null && o.getCustomerName().equals(n.getCustomerName());
                }
                @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Testimonial o = oldList.get(oldItemPosition);
                    Testimonial n = updated.get(newItemPosition);
                    return (o.getCustomerName() == null ? n.getCustomerName() == null : o.getCustomerName().equals(n.getCustomerName()))
                            && (o.getTestimonialText() == null ? n.getTestimonialText() == null : o.getTestimonialText().equals(n.getTestimonialText()));
                }
            });

            new Handler(Looper.getMainLooper()).post(() -> {
                testimonialList.clear();
                testimonialList.addAll(updated);
                diffResult.dispatchUpdatesTo(TestimonialsAdapter.this);
            });
        }).start();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCustomerImage;
        TextView tvCustomerName;
        TextView tvCustomerTitle;
        TextView tvTestimonialText;
        android.widget.RatingBar ratingBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCustomerImage = itemView.findViewById(R.id.ivCustomerImage);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvCustomerTitle = itemView.findViewById(R.id.tvCustomerTitle);
            tvTestimonialText = itemView.findViewById(R.id.tvTestimonialText);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
