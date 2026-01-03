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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.TimeZone;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Booking;
import vn.hcmute.busbooking.api.ApiClient;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookings;
    // pending countdowns in milliseconds keyed by booking id
    private Map<Integer, Long> pendingCountdowns = new HashMap<>();
    private OnBookingClickListener onBookingClickListener;

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    public void setOnBookingClickListener(OnBookingClickListener listener) {
        this.onBookingClickListener = listener;
    }

    public BookingAdapter(List<Booking> bookings) {
        this.bookings = bookings;
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
        return new BookingViewHolder(view);
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

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
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

            // setStatus needs booking id to lookup pending countdown and payment method
            setStatus(tvStatus, booking, pendingCountdowns);

            // Feedback status for completed trips
            tvFeedbackStatus.setVisibility(View.GONE);
            try {
                String status = booking.getStatus() == null ? "" : booking.getStatus();
                long arrivalTime = -1;
                try {
                    java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                    isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    java.util.Date d = isoFormat.parse(booking.getArrival_time());
                    if (d != null) arrivalTime = d.getTime();
                    else {
                        java.util.Date d2 = isoFormat.parse(booking.getDeparture_time());
                        if (d2 != null) arrivalTime = d2.getTime() + (4 * 60 * 60 * 1000L);
                    }
                } catch (Exception ignored) {}

                boolean isCompleted = false;
                if (status.equalsIgnoreCase("completed")) isCompleted = true;
                else if (status.equalsIgnoreCase("confirmed") && arrivalTime != -1 && System.currentTimeMillis() > arrivalTime) isCompleted = true;

                if (isCompleted) {
                    // Show feedback status and query backend whether user already reviewed this booking
                    tvFeedbackStatus.setVisibility(View.VISIBLE);
                    tvFeedbackStatus.setText("Đang kiểm tra...");
                    Context ctx = tvFeedbackStatus.getContext();
                    // Async call to check reviews for this booking
                    ApiClient.getClient().create(vn.hcmute.busbooking.api.ApiService.class)
                        .getReviewsForBooking(booking.getId())
                        .enqueue(new retrofit2.Callback<java.util.List<java.util.Map<String, Object>>>() {
                            @Override
                            public void onResponse(retrofit2.Call<java.util.List<java.util.Map<String, Object>>> call, retrofit2.Response<java.util.List<java.util.Map<String, Object>>> response) {
                                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                    tvFeedbackStatus.setText("Đã nhận xét");
                                    tvFeedbackStatus.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_status_confirmed));
                                    tvFeedbackStatus.setTextColor(ContextCompat.getColor(ctx, R.color.darkBlue));
                                    tvFeedbackStatus.setOnClickListener(v -> {
                                        // Open feedback details - maybe view their review
                                        Intent intent = new Intent(ctx, vn.hcmute.busbooking.activity.FeedbackActivity.class);
                                        intent.putExtra("booking_id", booking.getId());
                                        intent.putExtra("trip_id", booking.getTrip_id());
                                        ctx.startActivity(intent);
                                    });
                                } else {
                                    tvFeedbackStatus.setText("Đang chờ nhận xét");
                                    tvFeedbackStatus.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_status_pending));
                                    tvFeedbackStatus.setTextColor(ContextCompat.getColor(ctx, R.color.darkYellow));
                                    tvFeedbackStatus.setOnClickListener(v -> {
                                        Intent intent = new Intent(ctx, vn.hcmute.busbooking.activity.FeedbackActivity.class);
                                        intent.putExtra("booking_id", booking.getId());
                                        intent.putExtra("trip_id", booking.getTrip_id());
                                        ctx.startActivity(intent);
                                    });
                                }
                            }

                            @Override
                            public void onFailure(retrofit2.Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
                                tvFeedbackStatus.setText("Đang chờ nhận xét");
                                tvFeedbackStatus.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_status_pending));
                                tvFeedbackStatus.setTextColor(ContextCompat.getColor(ctx, R.color.darkYellow));
                                tvFeedbackStatus.setOnClickListener(v -> {
                                    Intent intent = new Intent(ctx, vn.hcmute.busbooking.activity.FeedbackActivity.class);
                                    intent.putExtra("booking_id", booking.getId());
                                    intent.putExtra("trip_id", booking.getTrip_id());
                                    ctx.startActivity(intent);
                                });
                            }
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
            } catch (ParseException e) {
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
            } catch (ParseException e) {
                return "";
            }
        }

        private String formatDuration(String minutesString) {
            if (minutesString == null) return "";
            try {
                int minutes = Integer.parseInt(minutesString);
                long hours = minutes / 60;
                return hours + " giờ";
            } catch (NumberFormatException e) {
                return "";
            }
        }

        private void setStatus(TextView tvStatus, Booking booking, Map<Integer, Long> pendingCountdowns) {
            Context context = tvStatus.getContext();
            String status = booking.getStatus();
            
            // 🔍 DEBUG: Log booking status details
            android.util.Log.d("BookingAdapter", String.format(
                "Booking #%d: status='%s', payment_method='%s', arrival='%s'",
                booking.getId(),
                status,
                booking.getPayment_method(),
                booking.getArrival_time()
            ));

            if (status == null) {
                tvStatus.setVisibility(View.GONE);
                return;
            }

            tvStatus.setVisibility(View.VISIBLE);
            int backgroundColor;
            int textColor;
            String statusText = status;
            
            // Logic xử lý status hiển thị
            // 1. Nếu status là "confirmed" nhưng đã quá giờ arrival -> "Đã đi"
            long arrivalTime = -1;
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = isoFormat.parse(booking.getArrival_time());
                if (d != null) arrivalTime = d.getTime();
                else {
                    // Fallback departure + 4h
                    Date d2 = isoFormat.parse(booking.getDeparture_time());
                    if (d2 != null) arrivalTime = d2.getTime() + (4 * 60 * 60 * 1000L);
                }
            } catch (Exception ignored) {}

            long now = System.currentTimeMillis();
            
            if (status.equalsIgnoreCase("completed")) {
                 statusText = "Đã đi";
                 backgroundColor = ContextCompat.getColor(context, R.color.lightBlue); 
                 textColor = ContextCompat.getColor(context, R.color.darkBlue);
            } else if (status.equalsIgnoreCase("confirmed")) {
                 if (arrivalTime != -1 && now > arrivalTime) {
                     statusText = "Đã đi"; // Confirmed but arrival time passed -> Completed
                     backgroundColor = ContextCompat.getColor(context, R.color.lightBlue); 
                     textColor = ContextCompat.getColor(context, R.color.darkBlue);
                 } else {
                     statusText = "Đã thanh toán";
                     backgroundColor = ContextCompat.getColor(context, R.color.lightGreen);
                     textColor = ContextCompat.getColor(context, R.color.darkGreen);
                 }
            } else if (status.equalsIgnoreCase("cancelled")) {
                 statusText = "Đã hủy";
                 backgroundColor = ContextCompat.getColor(context, R.color.lightRed);
                 textColor = ContextCompat.getColor(context, R.color.darkRed);
            } else if (status.equalsIgnoreCase("expired")) {
                 // Expired is also considered cancelled/invalid
                 statusText = "Đã hủy";
                 backgroundColor = ContextCompat.getColor(context, R.color.lightRed);
                 textColor = ContextCompat.getColor(context, R.color.darkRed);
            } else if (status.equalsIgnoreCase("pending")) {
                // Check payment method to determine behavior
                String paymentMethod = booking.getPayment_method();
                boolean isOfflinePayment = paymentMethod != null &&
                    (paymentMethod.toLowerCase().contains("cash") ||
                     paymentMethod.toLowerCase().contains("offline") ||
                     paymentMethod.toLowerCase().contains("cod") ||
                     paymentMethod.toLowerCase().contains("counter"));

                // Check if trip has ended
                boolean tripEnded = (arrivalTime != -1 && now > arrivalTime);

                if (tripEnded) {
                    // ✅ After trip ends: ALL pending bookings (online & offline) show "Đã hủy"
                    statusText = "Đã hủy";
                    backgroundColor = ContextCompat.getColor(context, R.color.lightRed);
                    textColor = ContextCompat.getColor(context, R.color.darkRed);
                } else {
                    // Before trip ends: different behavior for online vs offline
                    if (isOfflinePayment) {
                        // Offline payment: show "Chờ thanh toán" (awaiting payment)
                        statusText = "Chờ thanh toán";
                        backgroundColor = ContextCompat.getColor(context, R.color.lightYellow);
                        textColor = ContextCompat.getColor(context, R.color.darkYellow);
                    } else {
                        // Online payment: show countdown or "Chờ thanh toán"
                        Long rem = null;
                        if (pendingCountdowns != null) rem = pendingCountdowns.get(booking.getId());

                        if (rem != null && rem > 0) {
                            long seconds = rem / 1000;
                            long mm = seconds / 60;
                            long ss = seconds % 60;
                            statusText = String.format(Locale.getDefault(), "Chờ thanh toán (%02d:%02d)", mm, ss);
                        } else {
                            statusText = "Chờ thanh toán";
                        }
                        backgroundColor = ContextCompat.getColor(context, R.color.lightYellow);
                        textColor = ContextCompat.getColor(context, R.color.darkYellow);
                    }
                }
            } else {
                // Other statuses
                statusText = status;
                backgroundColor = ContextCompat.getColor(context, R.color.lightGray);
                textColor = ContextCompat.getColor(context, R.color.darkGray);
            }

            tvStatus.setText(statusText);
            GradientDrawable background = (GradientDrawable) tvStatus.getBackground();
            background.setColor(backgroundColor);
            tvStatus.setTextColor(textColor);
        }
    }
}
