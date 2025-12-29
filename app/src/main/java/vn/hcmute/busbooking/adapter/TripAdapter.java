package vn.hcmute.busbooking.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.database.FavoriteTripDatabase;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> tripList;
    private OnItemClickListener listener;
    private FavoriteTripDatabase favoriteTripDb;
    private Context context;
    private SessionManager sessionManager;

    public interface OnItemClickListener {
        void onItemClick(Trip trip);
    }

    public TripAdapter(Context context, List<Trip> tripList) {
        this.context = context;
        this.tripList = tripList;
        this.favoriteTripDb = new FavoriteTripDatabase(context);
        this.sessionManager = new SessionManager(context);
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

        // Calculate actual duration from departure and arrival times
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
                // Fallback to duration from trip object if parsing fails
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
            // Fallback to duration from trip object
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

        Integer seats = trip.getSeatsAvailable();
        if (seats != null) {
            holder.tvSeatsLeft.setText(String.format(Locale.getDefault(), "Còn %d chỗ", seats));
        } else {
            holder.tvSeatsLeft.setText("");
        }

        // Get current user ID
        Integer userId = sessionManager.getUserId();

        // Use the trip ID directly (matches database trip_id column)
        int tripId = trip.getId();

        if (userId != null) {
            // Update favorite icon based on current state
            boolean isFavorite = favoriteTripDb.isFavorite(tripId, userId);
            holder.btnFavorite.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);

            holder.btnFavorite.setOnClickListener(v -> {
                boolean isCurrentlyFavorite = favoriteTripDb.isFavorite(tripId, userId);
                if (isCurrentlyFavorite) {
                    favoriteTripDb.removeFavoriteTrip(tripId, userId);
                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                } else {
                    favoriteTripDb.addFavoriteTrip(trip, userId);
                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                }
            });
        } else {
            // User not logged in - show empty heart, clicking opens login
            holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            holder.btnFavorite.setOnClickListener(v -> {
                android.widget.Toast.makeText(context, "Vui lòng đăng nhập để yêu thích", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        holder.btnSelectSeats.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(trip);
            } else {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, vn.hcmute.busbooking.TripDetailActivity.class);
                intent.putExtra("trip", trip);
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
