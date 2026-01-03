package vn.hcmute.busbooking.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Booking;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.utils.ReviewStore;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookings;
    private Map<Integer, Long> pendingCountdowns = new HashMap<>();
    private OnBookingClickListener onBookingClickListener;
    private ReviewStore reviewStore;

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    public void setOnBookingClickListener(OnBookingClickListener listener) {
        this.onBookingClickListener = listener;
    }

    public BookingAdapter(List<Booking> bookings, Context context) {
        this.bookings = bookings;
        this.reviewStore = new ReviewStore(context);
    }

    public void updatePendingCountdowns(Map<Integer, Long> newCountdowns) {
        if (newCountdowns == null) newCountdowns = new HashMap<>();
        this.pendingCountdowns = newCountdowns;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view, reviewStore);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.bind(booking, pendingCountdowns);
        holder.itemView.setOnClickListener(v -> {
            if (onBookingClickListener != null) {
                onBookingClickListener.onBookingClick(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void updateBookings(List<Booking> newBookings) {
        this.bookings = newBookings;
        notifyDataSetChanged();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOperator, tvStatus, tvOrigin, tvDepartureTime, tvDestination, tvArrivalTime, tvDate, tvDuration;
        private final TextView tvFeedbackStatus;
        private final ReviewStore reviewStore;

        public BookingViewHolder(@NonNull View itemView, ReviewStore reviewStore) {
            super(itemView);
            this.reviewStore = reviewStore;
            tvOperator = itemView.findViewById(R.id.tvOperator);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvArrivalTime = itemView.findViewById(R.id.tvArrivalTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvFeedbackStatus = itemView.findViewById(R.id.tvFeedbackStatus);
        }

        public void bind(Booking booking, Map<Integer, Long> pendingCountdowns) {
            tvOperator.setText(booking.getOperator());
            tvOrigin.setText(booking.getOrigin());
            tvDestination.setText(booking.getDestination());

            tvDepartureTime.setText(formatTime(booking.getDeparture_time()));
            tvArrivalTime.setText(formatTime(booking.getArrival_time()));
            tvDate.setText(formatDate(booking.getDeparture_time()));
            tvDuration.setText(formatDuration(booking.getDuration()));

            setStatus(tvStatus, booking, pendingCountdowns);

            tvFeedbackStatus.setVisibility(View.GONE);
            try {
                String status = booking.getStatus() == null ? "" : booking.getStatus();
                long arrivalTime = -1;
                try {
                    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date d = isoFormat.parse(booking.getArrival_time());
                    if (d != null) arrivalTime = d.getTime();
                    else {
                        Date d2 = isoFormat.parse(booking.getDeparture_time());
                        if (d2 != null) arrivalTime = d2.getTime() + (4 * 60 * 60 * 1000L);
                    }
                } catch (Exception ignored) {}

                boolean isCompleted = false;
                if (status.equalsIgnoreCase("completed")) isCompleted = true;
                else if (status.equalsIgnoreCase("confirmed") && arrivalTime != -1 && System.currentTimeMillis() > arrivalTime) isCompleted = true;

                if (isCompleted) {
                    tvFeedbackStatus.setVisibility(View.VISIBLE);
                    Context ctx = tvFeedbackStatus.getContext();
                    
                    if (reviewStore != null && reviewStore.hasLocalReview(booking.getId())) {
                        tvFeedbackStatus.setText("Đã nhận xét");
                        tvFeedbackStatus.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_status_confirmed));
                        tvFeedbackStatus.setTextColor(ContextCompat.getColor(ctx, R.color.darkBlue));
                    } else {
                        tvFeedbackStatus.setText("Đang chờ nhận xét");
                        tvFeedbackStatus.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_status_pending));
                        tvFeedbackStatus.setTextColor(ContextCompat.getColor(ctx, R.color.darkYellow));
                    }
                    
                    tvFeedbackStatus.setOnClickListener(v -> {
                        Intent intent = new Intent(ctx, vn.hcmute.busbooking.activity.FeedbackActivity.class);
                        intent.putExtra("booking_id", booking.getId());
                        intent.putExtra("trip_id", booking.getTrip_id());
                        ctx.startActivity(intent);
                    });
                }
            } catch (Exception ignored) {}
        }

        private String formatTime(String isoString) {
            if (isoString == null) return "";
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = isoFormat.parse(isoString);
                if (date == null) return "";
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return timeFormat.format(date);
            } catch (Exception e) {
                return "";
            }
        }

        private String formatDate(String isoString) {
            if (isoString == null) return "";
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = isoFormat.parse(isoString);
                if (date == null) return "";
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
                return dateFormat.format(date);
            } catch (Exception e) {
                return "";
            }
        }

        private String formatDuration(String minutesString) {
            if (minutesString == null) return "";
            try {
                int minutes = Integer.parseInt(minutesString);
                long hours = minutes / 60;
                return hours + " giờ";
            } catch (Exception e) {
                return "";
            }
        }

        private void setStatus(TextView tvStatus, Booking booking, Map<Integer, Long> pendingCountdowns) {
            Context context = tvStatus.getContext();
            String status = booking.getStatus();
            if (status == null) {
                tvStatus.setVisibility(View.GONE);
                return;
            }
            tvStatus.setVisibility(View.VISIBLE);
            int backgroundColor;
            int textColor;
            String statusText = status;
            
            long arrivalTime = -1;
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = isoFormat.parse(booking.getArrival_time());
                if (d != null) arrivalTime = d.getTime();
            } catch (Exception ignored) {}

            long now = System.currentTimeMillis();
            if (status.equalsIgnoreCase("completed") || (status.equalsIgnoreCase("confirmed") && arrivalTime != -1 && now > arrivalTime)) {
                 statusText = "Đã đi";
                 backgroundColor = ContextCompat.getColor(context, R.color.lightBlue); 
                 textColor = ContextCompat.getColor(context, R.color.darkBlue);
            } else if (status.equalsIgnoreCase("confirmed")) {
                 statusText = "Đã thanh toán";
                 backgroundColor = ContextCompat.getColor(context, R.color.lightGreen);
                 textColor = ContextCompat.getColor(context, R.color.darkGreen);
            } else if (status.equalsIgnoreCase("cancelled") || status.equalsIgnoreCase("expired")) {
                 statusText = "Đã hủy";
                 backgroundColor = ContextCompat.getColor(context, R.color.lightRed);
                 textColor = ContextCompat.getColor(context, R.color.darkRed);
            } else if (status.equalsIgnoreCase("pending")) {
                statusText = "Chờ thanh toán";
                Long rem = pendingCountdowns != null ? pendingCountdowns.get(booking.getId()) : null;
                if (rem != null && rem > 0) {
                    long mm = rem / 1000 / 60;
                    long ss = (rem / 1000) % 60;
                    statusText = String.format(Locale.getDefault(), "Chờ thanh toán (%02d:%02d)", mm, ss);
                }
                backgroundColor = ContextCompat.getColor(context, R.color.lightYellow);
                textColor = ContextCompat.getColor(context, R.color.darkYellow);
            } else {
                backgroundColor = ContextCompat.getColor(context, R.color.lightGray);
                textColor = ContextCompat.getColor(context, R.color.darkGray);
            }

            tvStatus.setText(statusText);
            GradientDrawable background = (GradientDrawable) tvStatus.getBackground();
            if (background != null) background.setColor(backgroundColor);
            tvStatus.setTextColor(textColor);
        }
    }
}
