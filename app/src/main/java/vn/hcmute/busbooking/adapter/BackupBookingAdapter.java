package vn.hcmute.busbooking.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Booking;

// DEPRECATED: This adapter is not fully compatible with the item_booking layout.
public class BackupBookingAdapter extends RecyclerView.Adapter<BackupBookingAdapter.BookingViewHolder> {

    private List<Booking> bookings;
    private OnCancelListener cancelListener;
    private OnItemClickListener itemClickListener;
    private Context context;

    public interface OnCancelListener {
        void onCancel(Booking booking);
    }

    public interface OnItemClickListener {
        void onItemClick(Booking booking);
    }

    public BackupBookingAdapter(List<Booking> bookings, OnCancelListener listener) {
        this.bookings = bookings;
        this.cancelListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);

        String origin = booking.getOrigin();
        String destination = booking.getDestination();
        String status = booking.getStatus();
        String operator = booking.getOperator(); // Assuming Booking model has getOperator()

        // The layout has separate TextViews for origin and destination
        if (holder.tvOrigin != null) {
            holder.tvOrigin.setText(origin);
        }
        if (holder.tvDestination != null) {
            holder.tvDestination.setText(destination);
        }
        if (holder.tvOperator != null) {
            holder.tvOperator.setText(operator);
        }

        if (holder.tvStatus != null) {
            holder.tvStatus.setText(getStatusText(status));
            updateStatusBackground(holder.tvStatus, status);
        }

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(booking);
            }
        });
    }

    private void updateStatusBackground(TextView textView, String status) {
        if (textView == null || textView.getBackground() == null) return;
        int colorResId;
        if (status == null) status = "";
        switch (status) {
            case "confirmed":
                colorResId = R.color.colorPrimary;
                break;
            case "cancelled":
            case "refunded":
                colorResId = R.color.darkRed;
                break;
            case "pending":
                colorResId = R.color.colorAccent;
                break;
            default:
                colorResId = R.color.textSecondary;
                break;
        }
        if (textView.getBackground() instanceof GradientDrawable) {
            GradientDrawable background = (GradientDrawable) textView.getBackground();
            background.setColor(ContextCompat.getColor(context, colorResId));
        } else {
             // Fallback for other drawables if needed
             textView.setBackgroundColor(ContextCompat.getColor(context, colorResId));
        }
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    public void updateBookings(List<Booking> newBookings) {
        this.bookings = newBookings;
        notifyDataSetChanged();
    }

    private String getStatusText(String status) {
        if (status == null) return "";
        switch (status) {
            case "confirmed": return "Đã thanh toán";
            case "pending": return "Chờ thanh toán";
            case "cancelled": return "Đã hủy";
            case "refunded": return "Đã hoàn tiền";
            default: return status;
        }
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        // Use IDs that actually exist in item_booking.xml
        TextView tvOrigin, tvDestination, tvStatus, tvOperator;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOperator = itemView.findViewById(R.id.tvOperator);
        }
    }
}
