package vn.hcmute.busbooking.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.activity.TripDetailActivity;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> tripList;
    private OnItemClickListener listener;
    private Context context;
    private SessionManager sessionManager;
    private ApiService apiService;
    private List<Integer> favoriteTripIds;

    public interface OnItemClickListener {
        void onItemClick(Trip trip);
    }

    public TripAdapter(Context context, List<Trip> tripList, List<Integer> favoriteTripIds) {
        this.context = context;
        this.tripList = tripList;
        this.sessionManager = new SessionManager(context);
        this.apiService = ApiClient.getClient().create(ApiService.class);
        this.favoriteTripIds = favoriteTripIds;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateTrips(List<Trip> newTrips) {
        this.tripList = newTrips;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        holder.tvOperator.setText(trip.getOperator());
        holder.tvVehicleType.setText(trip.getBusType());
        holder.tvPrice.setText(String.format(Locale.GERMAN, "%,.0fđ", trip.getPrice()));

        String departureTimeStr = trip.getDepartureTime();
        String arrivalTimeStr = trip.getArrivalTime();

        if (departureTimeStr != null && departureTimeStr.length() > 16) {
            String time = departureTimeStr.substring(11, 16);
            holder.tvDepartureTime.setText(time);

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE, dd MMM", Locale.ENGLISH);
            try {
                Date date = inputFormat.parse(departureTimeStr);
                if (date != null) {
                    holder.tvDate.setText(outputFormat.format(date));
                }
            } catch (ParseException e) {
                e.printStackTrace();
                holder.tvDate.setText("");
            }

        } else {
            holder.tvDepartureTime.setText("N/A");
            holder.tvDate.setText("");
        }

        if (arrivalTimeStr != null && arrivalTimeStr.length() > 16) {
            String time = arrivalTimeStr.substring(11, 16);
            holder.tvArrivalTime.setText(time);
        } else {
            holder.tvArrivalTime.setText("N/A");
        }

        holder.tvOrigin.setText(trip.getFromLocation());
        holder.tvDestination.setText(trip.getToLocation());

        if (departureTimeStr != null && arrivalTimeStr != null) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date departureDate = format.parse(departureTimeStr);
                Date arrivalDate = format.parse(arrivalTimeStr);

                if (departureDate != null && arrivalDate != null) {
                    long durationMillis = arrivalDate.getTime() - departureDate.getTime();
                    double durationHours = durationMillis / (1000.0 * 60 * 60);

                    if (durationHours % 1 == 0) {
                        holder.tvDuration.setText(String.format(Locale.getDefault(), "%.0f giờ", durationHours));
                    } else {
                        holder.tvDuration.setText(String.format(Locale.getDefault(), "%.1f giờ", durationHours));
                    }
                } else {
                    holder.tvDuration.setText("");
                }
            } catch (ParseException e) {
                e.printStackTrace();
                Double duration = trip.getDurationHours();
                if (duration != null) {
                    if (duration % 1 == 0) {
                        holder.tvDuration.setText(String.format(Locale.getDefault(), "%.0f giờ", duration));
                    } else {
                        holder.tvDuration.setText(String.format(Locale.getDefault(), "%.1f giờ", duration));
                    }
                } else {
                    holder.tvDuration.setText("");
                }
            }
        } else {
            Double duration = trip.getDurationHours();
            if (duration != null) {
                if (duration % 1 == 0) {
                    holder.tvDuration.setText(String.format(Locale.getDefault(), "%.0f giờ", duration));
                } else {
                    holder.tvDuration.setText(String.format(Locale.getDefault(), "%.1f giờ", duration));
                }
            } else {
                holder.tvDuration.setText("");
            }
        }

        // Try to fetch latest seats available from API for more accurate display.
        holder.tvSeatsLeft.setText("");
        holder.itemView.setTag("trip_" + trip.getId());
        apiService.getTripDetails(trip.getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                Object seatsObj = response.body().get("seats_available");
                Integer seatsNum = null;
                if (seatsObj instanceof Number) seatsNum = ((Number) seatsObj).intValue();
                else if (seatsObj instanceof String) {
                    try { seatsNum = Integer.parseInt((String) seatsObj); } catch (Exception ignored) {}
                }

                // Ensure this ViewHolder still represents the same trip (avoid recycling issues)
                Object tag = holder.itemView.getTag();
                if (tag != null && tag.equals("trip_" + trip.getId())) {
                    if (seatsNum != null) {
                        holder.tvSeatsLeft.setText(String.format(Locale.getDefault(), "Còn %d chỗ", seatsNum));
                    } else if (trip.getSeatsAvailable() != null) {
                        holder.tvSeatsLeft.setText(String.format(Locale.getDefault(), "Còn %d chỗ", trip.getSeatsAvailable()));
                    } else if (trip.getSeatsTotal() != null) {
                        // Fallback to seats_total if available
                        holder.tvSeatsLeft.setText(String.format(Locale.getDefault(), "Còn %d chỗ", trip.getSeatsTotal()));
                    } else {
                        holder.tvSeatsLeft.setText("");
                    }
                }

                // If seatsNum is null or inconsistent with seats_total, fetch authoritative available seats list
                try {
                    Integer total = trip.getSeatsTotal();
                    Integer availableReported = seatsNum != null ? seatsNum : trip.getSeatsAvailable();
                    boolean needsVerify = (availableReported == null && total != null) ||
                            (availableReported != null && total != null && Math.abs(availableReported - total) > 2);

                    if (needsVerify) {
                        // Use the seats endpoint which returns a list of Seat objects and count unbooked seats
                        apiService.getSeats(trip.getId()).enqueue(new Callback<java.util.List<vn.hcmute.busbooking.model.Seat>>() {
                            @Override
                            public void onResponse(Call<java.util.List<vn.hcmute.busbooking.model.Seat>> call2, Response<java.util.List<vn.hcmute.busbooking.model.Seat>> resp2) {
                                if (!resp2.isSuccessful() || resp2.body() == null) return;
                                int availableCount = 0;
                                try {
                                    for (vn.hcmute.busbooking.model.Seat s : resp2.body()) {
                                        if (s == null) continue;
                                        // seat.isBooked() indicates booked seats; count those that are NOT booked
                                        if (!s.isBooked()) availableCount++;
                                    }
                                } catch (Exception ignored) {}
                                Object tag2 = holder.itemView.getTag();
                                if (tag2 != null && tag2.equals("trip_" + trip.getId())) {
                                    holder.tvSeatsLeft.setText(String.format(Locale.getDefault(), "Còn %d chỗ", availableCount));
                                }
                            }

                            @Override
                            public void onFailure(Call<java.util.List<vn.hcmute.busbooking.model.Seat>> call2, Throwable t2) {
                                // ignore - keep previous display
                            }
                        });
                    }
                } catch (Exception ignored) {}

            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Fallback to local value
                if (holder.itemView.getTag() != null && holder.itemView.getTag().equals("trip_" + trip.getId())) {
                    if (trip.getSeatsAvailable() != null) {
                        holder.tvSeatsLeft.setText(String.format(Locale.getDefault(), "Còn %d chỗ", trip.getSeatsAvailable()));
                    } else {
                        holder.tvSeatsLeft.setText("");
                    }
                }
            }
        });

        Integer userId = sessionManager.getUserId();
        int tripId = trip.getId();

        if (userId != null) {
            boolean isFavorite = favoriteTripIds.contains(tripId);
            holder.btnFavorite.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);

            holder.btnFavorite.setOnClickListener(v -> {
                boolean isCurrentlyFavorite = favoriteTripIds.contains(tripId);
                if (isCurrentlyFavorite) {
                    apiService.removeFavorite(userId, tripId).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                favoriteTripIds.remove(Integer.valueOf(tripId));
                                holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                                Toast.makeText(context, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            // Do nothing
                        }
                    });
                } else {
                    Map<String, Integer> body = new HashMap<>();
                    body.put("trip_id", tripId);
                    apiService.addFavorite(userId, body).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                favoriteTripIds.add(tripId);
                                holder.btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                                Toast.makeText(context, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            // Do nothing
                        }
                    });
                }
            });
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            holder.btnFavorite.setOnClickListener(v -> {
                Toast.makeText(context, "Vui lòng đăng nhập để yêu thích", Toast.LENGTH_SHORT).show();
            });
        }

        holder.btnSelectSeats.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(trip);
            } else {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, TripDetailActivity.class);
                intent.putExtra("trip_id", trip.getId());
                ctx.startActivity(intent);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tripList != null ? tripList.size() : 0;
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvOperator, tvVehicleType, tvPrice, tvDepartureTime, tvOrigin,
                tvDuration, tvArrivalTime, tvDestination, tvDate, tvSeatsLeft;
        ImageButton btnFavorite;
        MaterialButton btnSelectSeats;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOperator = itemView.findViewById(R.id.tvOperator);
            tvVehicleType = itemView.findViewById(R.id.tvVehicleType);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvArrivalTime = itemView.findViewById(R.id.tvArrivalTime);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSeatsLeft = itemView.findViewById(R.id.tvSeatsLeft);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnSelectSeats = itemView.findViewById(R.id.btnSelectSeats);
        }
    }
}
