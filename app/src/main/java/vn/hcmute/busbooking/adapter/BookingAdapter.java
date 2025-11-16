package vn.hcmute.busbooking.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Booking;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

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

    public BookingAdapter(List<Booking> bookings, OnCancelListener listener) {
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
        String seatLabel = booking.getSeat_label();
        String status = booking.getStatus();
        String departureTimeStr = booking.getDeparture_time();
        int price = booking.getPrice_paid();

        holder.tvRoute.setText(origin + " → " + destination);
        holder.tvSeat.setText("Ghế: " + seatLabel);
        holder.tvPrice.setText(String.format(Locale.GERMANY, "%,.0fđ", (double) price));

        holder.tvDeparture.setText(formatDateTime(departureTimeStr));

        holder.tvStatus.setText(getStatusText(status));
        updateStatusBackground(holder.tvStatus, status);

        if ("confirmed".equals(status) || "pending".equals(status)) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> {
                if (cancelListener != null) {
                    cancelListener.onCancel(booking);
                }
            });
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(booking);
            }
        });
    }

    private String formatDateTime(String isoString) {
        if (isoString == null) return "N/A";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoString);

            SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
            return displayFormat.format(date);
        } catch (ParseException e) {
            return isoString; // fallback to raw string
        }
    }

    private void updateStatusBackground(TextView textView, String status) {
        int colorResId;
        switch (status) {
            case "confirmed":
                colorResId = R.color.colorPrimary;
                break;
            case "cancelled":
            case "refunded":
                colorResId = R.color.colorError;
                break;
            case "pending":
                colorResId = R.color.colorAccent;
                break;
            default:
                colorResId = R.color.textSecondary;
                break;
        }
        GradientDrawable background = (GradientDrawable) textView.getBackground();
        background.setColor(ContextCompat.getColor(context, colorResId));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void updateBookings(List<Booking> newBookings) {
        this.bookings = newBookings;
        notifyDataSetChanged();
    }

    private String getStatusText(String status) {
        switch (status) {
            case "confirmed": return "Đã xác nhận";
            case "pending": return "Chờ xác nhận";
            case "cancelled": return "Đã hủy";
            case "refunded": return "Đã hoàn tiền";
            default: return status;
        }
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvSeat, tvStatus, tvDeparture, tvPrice;
        Button btnCancel;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvSeat = itemView.findViewById(R.id.tvSeat);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDeparture = itemView.findViewById(R.id.tvDeparture);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}
