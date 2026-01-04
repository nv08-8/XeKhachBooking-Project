package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Feedback;

public class FeedbackAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_PENDING = 0;
    private static final int TYPE_REVIEWED = 1;

    private List<Feedback> feedbackList;
    private int type;
    private OnFeedbackClickListener listener;

    public interface OnFeedbackClickListener {
        void onFeedbackClick(Feedback feedback);
    }

    public FeedbackAdapter(List<Feedback> feedbackList, int type, OnFeedbackClickListener listener) {
        this.feedbackList = feedbackList;
        this.type = type;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (type == TYPE_PENDING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback_pending, parent, false);
            return new PendingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback_reviewed, parent, false);
            return new ReviewedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Feedback feedback = feedbackList.get(position);
        String route = feedback.getOrigin() + " -> " + feedback.getDestination();
        
        if (holder instanceof PendingViewHolder) {
            PendingViewHolder vh = (PendingViewHolder) holder;
            vh.tvRoute.setText(route);
            vh.tvOperator.setText(feedback.getOperator());
            vh.tvDepartureTime.setText("Khởi hành: " + feedback.getDepartureTime());
            vh.btnFeedback.setOnClickListener(v -> listener.onFeedbackClick(feedback));
        } else if (holder instanceof ReviewedViewHolder) {
            ReviewedViewHolder vh = (ReviewedViewHolder) holder;
            vh.tvRoute.setText(route);
            vh.ratingBar.setRating(feedback.getRating());
            vh.tvComment.setText(feedback.getComment());
            vh.tvFeedbackDate.setText("Đã nhận xét lúc: " + feedback.getFeedbackDate());
        }
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    public static class PendingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvOperator, tvDepartureTime;
        Button btnFeedback;

        public PendingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvOperator = itemView.findViewById(R.id.tvOperator);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            btnFeedback = itemView.findViewById(R.id.btnFeedback);
        }
    }

    public static class ReviewedViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvComment, tvFeedbackDate;
        RatingBar ratingBar;

        public ReviewedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvFeedbackDate = itemView.findViewById(R.id.tvFeedbackDate);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
