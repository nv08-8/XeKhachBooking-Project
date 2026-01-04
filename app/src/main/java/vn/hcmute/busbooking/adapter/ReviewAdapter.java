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

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Feedback> feedbackList;
    private OnReviewActionListener listener;

    public interface OnReviewActionListener {
        void onEdit(Feedback feedback);
        void onDelete(Feedback feedback);
        void onReply(Feedback feedback);
    }

    public ReviewAdapter(List<Feedback> feedbackList, OnReviewActionListener listener) {
        this.feedbackList = feedbackList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Feedback feedback = feedbackList.get(position);

        holder.tvUserName.setText(feedback.getUserName() != null ? feedback.getUserName() : "Khách");
        holder.tvRouteInfo.setText(feedback.getRouteInfo() != null ? feedback.getRouteInfo() : "");
        holder.tvTripDate.setText(feedback.getTripDate() != null ? feedback.getTripDate() : "");
        holder.tvComment.setText(feedback.getComment() != null ? feedback.getComment() : "Không có bình luận");
        holder.ratingBar.setRating(feedback.getRating());
        holder.ratingBar.setIsIndicator(true);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(feedback);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(feedback);
        });

        holder.btnReply.setOnClickListener(v -> {
            if (listener != null) listener.onReply(feedback);
        });
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvRouteInfo, tvTripDate, tvComment;
        RatingBar ratingBar;
        Button btnEdit, btnDelete, btnReply;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvRouteInfo = itemView.findViewById(R.id.tvRouteInfo);
            tvTripDate = itemView.findViewById(R.id.tvTripDate);
            tvComment = itemView.findViewById(R.id.tvComment);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnReply = itemView.findViewById(R.id.btnReply);
        }
    }
}