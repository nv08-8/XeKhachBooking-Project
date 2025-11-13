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
        holder.tvTestimonialText.setText(testimonial.getTestimonialText());
        holder.ivCustomerImage.setImageResource(testimonial.getCustomerImage());
    }

    @Override
    public int getItemCount() {
        return testimonialList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCustomerImage;
        TextView tvCustomerName;
        TextView tvTestimonialText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCustomerImage = itemView.findViewById(R.id.ivCustomerImage);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvTestimonialText = itemView.findViewById(R.id.tvTestimonialText);
        }
    }
}
