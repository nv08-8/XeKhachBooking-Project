package vn.hcmute.busbooking;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.activity.SeatSelectionActivity;
import vn.hcmute.busbooking.adapter.ImageSliderAdapter;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.database.FavoriteTripDatabase;
import vn.hcmute.busbooking.model.Trip;
import vn.hcmute.busbooking.utils.SessionManager;

public class TripDetailActivity extends AppCompatActivity {

    private static final String TAG = "TripDetailActivity";

    private ViewPager2 imageViewPager;
    private LinearLayout dotsIndicator;

    private TextView txtOperatorName, txtBusType, txtPrice;
    private Button btnBookNow;
    private ViewGroup layoutAmenities, layoutTimeline;
    private TextView txtRating, txtReviews;
    private ProgressBar progressBar5, progressBar4, progressBar3, progressBar2, progressBar1;
    private TextView txtPercent5, txtPercent4, txtPercent3, txtPercent2, txtPercent1;
    private ImageView btnFavorite;

    private Trip trip;
    private FavoriteTripDatabase favoriteTripDatabase;
    private SessionManager sessionManager;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);
        initViews();

        favoriteTripDatabase = new FavoriteTripDatabase(this);
        sessionManager = new SessionManager(this);

        trip = getIntent().getParcelableExtra("trip");

        if (trip != null) {
            // Display basic trip info immediately from the Trip object
            displayBasicTripInfo();
            updateFavoriteIcon();

            // Then fetch additional details (amenities, timeline, reviews) from API
            fetchTripDetails(trip.getId());
        } else {
            Toast.makeText(this, "Không có thông tin chuyến đi", Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (trip != null) {
            updateFavoriteIcon();
        }
    }

    private void initViews() {
        imageViewPager = findViewById(R.id.imageViewPager);
        dotsIndicator = findViewById(R.id.dotsIndicator);
        txtOperatorName = findViewById(R.id.txtOperatorName);
        txtBusType = findViewById(R.id.txtBusType);
        txtPrice = findViewById(R.id.txtPrice);
        btnBookNow = findViewById(R.id.btnBookNow);
        layoutAmenities = findViewById(R.id.layoutAmenities);
        layoutTimeline = findViewById(R.id.layoutTimeline);
        txtRating = findViewById(R.id.txtRating);
        txtReviews = findViewById(R.id.txtReviews);
        progressBar1 = findViewById(R.id.progressBar1);
        progressBar2 = findViewById(R.id.progressBar2);
        progressBar3 = findViewById(R.id.progressBar3);
        progressBar4 = findViewById(R.id.progressBar4);
        progressBar5 = findViewById(R.id.progressBar5);
        txtPercent1 = findViewById(R.id.txtPercent1);
        txtPercent2 = findViewById(R.id.txtPercent2);
        txtPercent3 = findViewById(R.id.txtPercent3);
        txtPercent4 = findViewById(R.id.txtPercent4);
        txtPercent5 = findViewById(R.id.txtPercent5);
        btnFavorite = findViewById(R.id.btnFavorite);
    }

    private void displayBasicTripInfo() {
        Log.d(TAG, "=== displayBasicTripInfo() START ===");
        Log.d(TAG, "Trip ID: " + trip.getId());
        Log.d(TAG, "Trip RouteID: " + trip.getRouteId());
        Log.d(TAG, "Trip Operator: " + trip.getOperator());
        Log.d(TAG, "Trip BusType: " + trip.getBusType());
        Log.d(TAG, "Trip Price: " + trip.getPrice());
        Log.d(TAG, "Trip Origin: " + trip.getOrigin());
        Log.d(TAG, "Trip Destination: " + trip.getDestination());

        // Display operator name and bus type immediately
        if (trip.getOperator() != null) {
            txtOperatorName.setText(trip.getOperator());
            Log.d(TAG, "Set txtOperatorName to: " + trip.getOperator());
        }
        if (trip.getBusType() != null) {
            txtBusType.setText(trip.getBusType());
            Log.d(TAG, "Set txtBusType to: " + trip.getBusType());
        }

        // Display price
        txtPrice.setText(String.format(Locale.getDefault(), "%,.0f ₫", trip.getPrice()));
        Log.d(TAG, "Set txtPrice to: " + String.format(Locale.getDefault(), "%,.0f ₫", trip.getPrice()));

        // Setup book button with basic info
        btnBookNow.setOnClickListener(v -> {
            Intent intent = new Intent(TripDetailActivity.this, SeatSelectionActivity.class);
            intent.putExtra("trip", trip);
            startActivity(intent);
        });

        // Setup image slider with placeholder drawable (no external URL to avoid Glide errors)
        setupImageSliderWithPlaceholder();

        Log.d(TAG, "=== displayBasicTripInfo() END ===");
    }

    private void fetchTripDetails(int tripId) {
        Log.d(TAG, "Fetching additional trip details for ID: " + tripId);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getTripDetails(tripId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Log.d(TAG, "Response received - Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseBody = response.body();
                    Log.d(TAG, "Response body keys: " + responseBody.keySet());

                    // Check if response has 'data' field directly (some APIs return data directly)
                    Object dataObj = responseBody.get("data");

                    if (dataObj != null) {
                        Log.d(TAG, "Found 'data' field in response");
                        Gson gson = new Gson();
                        Type tripType = new TypeToken<Map<String, Object>>() {}.getType();
                        Map<String, Object> data = gson.fromJson(gson.toJson(dataObj), tripType);
                        updateAdditionalTripDetails(data);
                    } else if (responseBody.containsKey("operator")) {
                        // Response IS the data (no wrapper)
                        Log.d(TAG, "Response body IS the trip data (no wrapper)");
                        updateAdditionalTripDetails(responseBody);
                    } else {
                        Log.e(TAG, "Response format not recognized. Keys: " + responseBody.keySet());
                        // Still show the basic info we already have
                    }
                } else {
                    Log.e(TAG, "Response not successful or body is null. Code: " + response.code());
                    // Still show the basic info we already have
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch additional trip details", t);
                // Still show the basic info we already have
            }
        });
    }

    private void updateAdditionalTripDetails(Map<String, Object> data) {
        // Update bus images if available
        Object imageUrlsObj = data.get("bus_image_url");
        List<String> imageUrls = new ArrayList<>();
        if (imageUrlsObj instanceof String) {
            Type listType = new TypeToken<ArrayList<String>>() {
            }.getType();
            imageUrls = new Gson().fromJson((String) imageUrlsObj, listType);
        } else if (imageUrlsObj instanceof List) {
            imageUrls.addAll((List<String>) imageUrlsObj);
        }

        // Filter out tiktok links
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            imageUrls = imageUrls.stream()
                    .filter(url -> url != null && !url.contains("tiktok.com"))
                    .collect(Collectors.toList());
        }

        if (!imageUrls.isEmpty()) {
            // Show image slider when images are available
            imageViewPager.setVisibility(View.VISIBLE);
            dotsIndicator.setVisibility(View.VISIBLE);
            setupImageSlider(imageUrls);
        }

        // Update amenities if available
        if (data.get("amenities") instanceof Map) {
            displayAmenities((Map<String, Boolean>) data.get("amenities"));
        }

        // Update timeline if available
        if (data.get("timeline") instanceof List) {
            displayTimeline((List<Map<String, Object>>) data.get("timeline"));
        }

        // Update reviews if available
        if (data.get("reviews") instanceof List) {
            displayReviews((List<Map<String, Object>>) data.get("reviews"));
        }

        // Update seat layout if available for booking
        if (data.get("seat_layout") != null) {
            String seatLayoutJson = new Gson().toJson(data.get("seat_layout"));
            trip.setSeatLayout(seatLayoutJson);

            // Update book button with seat layout
            btnBookNow.setOnClickListener(v -> {
                Intent intent = new Intent(TripDetailActivity.this, SeatSelectionActivity.class);
                intent.putExtra("trip", trip);
                startActivity(intent);
            });
        }
    }

    private void displayAmenities(Map<String, Boolean> amenities) {
        layoutAmenities.removeAllViews();
        for (Map.Entry<String, Boolean> entry : amenities.entrySet()) {
            if (entry.getValue()) {
                addAmenityView(getAmenityName(entry.getKey()));
            }
        }
    }

    private String getAmenityName(String key) {
        switch (key) {
            case "wifi":
                return "Wifi";
            case "water":
                return "Nước uống";
            case "ac":
                return "Điều hòa";
            case "wc":
                return "WC";
            case "tv":
                return "TV";
            case "charging":
                return "Cổng sạc";
            default:
                return key;
        }
    }

    private void addAmenityView(String name) {
        View amenityView = getLayoutInflater().inflate(R.layout.item_amenity, layoutAmenities, false);
        ((TextView) amenityView.findViewById(R.id.txtAmenity)).setText(name);
        layoutAmenities.addView(amenityView);
    }

    private void setupImageSliderWithPlaceholder() {
        // Don't load any images initially to avoid Glide errors
        // Just show empty slider, images will be loaded when API returns data
        imageViewPager.setVisibility(View.GONE);
        dotsIndicator.setVisibility(View.GONE);
    }

    private void displayTimeline(List<Map<String, Object>> timelineData) {
        layoutTimeline.removeAllViews();
        for (int i = 0; i < timelineData.size(); i++) {
            Map<String, Object> stop = timelineData.get(i);
            addTimelineStop(
                    (String) stop.get("location"),
                    (String) stop.get("time"),
                    (String) stop.get("description"),
                    i == 0
            );
        }
    }

    private void addTimelineStop(String location, String time, String description, boolean isFirst) {
        View stopView = getLayoutInflater().inflate(R.layout.item_timeline_stop, layoutTimeline, false);
        ((TextView) stopView.findViewById(R.id.txtStopLocation)).setText(location);
        ((TextView) stopView.findViewById(R.id.txtStopDescription)).setText(description);
        try {
            ((TextView) stopView.findViewById(R.id.txtStopTime)).setText(formatTime(time));
        } catch (Exception e) {
            ((TextView) stopView.findViewById(R.id.txtStopTime)).setText("N/A");
        }
        stopView.findViewById(R.id.lineTop).setVisibility(isFirst ? View.INVISIBLE : View.VISIBLE);
        layoutTimeline.addView(stopView);
    }

    private void displayReviews(List<Map<String, Object>> reviews) {
        int totalReviews = reviews.size();
        if (totalReviews == 0) {
            txtRating.setText("0.0");
            txtReviews.setText("(0 đánh giá)");
            setRatingBar(progressBar5, txtPercent5, 0);
            setRatingBar(progressBar4, txtPercent4, 0);
            setRatingBar(progressBar3, txtPercent3, 0);
            setRatingBar(progressBar2, txtPercent2, 0);
            setRatingBar(progressBar1, txtPercent1, 0);
            return;
        }

        double totalRatingSum = 0;
        int[] ratingCounts = new int[5]; // Index 0 for 1 star, up to 4 for 5 stars

        for (Map<String, Object> review : reviews) {
            Object ratingObj = review.get("rating");
            if (ratingObj instanceof Number) {
                int rating = ((Number) ratingObj).intValue();
                if (rating >= 1 && rating <= 5) {
                    totalRatingSum += rating;
                    ratingCounts[rating - 1]++;
                }
            }
        }

        double averageRating = totalRatingSum / totalReviews;
        txtRating.setText(new DecimalFormat("0.0").format(averageRating));
        txtReviews.setText(String.format(Locale.getDefault(), "(%d đánh giá)", totalReviews));

        setRatingBar(progressBar5, txtPercent5, (ratingCounts[4] * 100) / totalReviews);
        setRatingBar(progressBar4, txtPercent4, (ratingCounts[3] * 100) / totalReviews);
        setRatingBar(progressBar3, txtPercent3, (ratingCounts[2] * 100) / totalReviews);
        setRatingBar(progressBar2, txtPercent2, (ratingCounts[1] * 100) / totalReviews);
        setRatingBar(progressBar1, txtPercent1, (ratingCounts[0] * 100) / totalReviews);
    }

    private String formatTime(String isoString) {
        if (isoString == null) return "";
        try {
            // Try parsing with milliseconds and Z (UTC format) first
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = inputFormat.parse(isoString);
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } catch (Exception e) {
            try {
                // Try parsing without milliseconds (local format)
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(isoString);
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
            } catch (Exception e2) {
                return "";
            }
        }
    }

    private void setupImageSlider(List<String> images) {
        imageViewPager.setAdapter(new ImageSliderAdapter(this, images));
        if (images.size() > 1) {
            setupDotsIndicator(images.size());
            dotsIndicator.setVisibility(View.VISIBLE);
            imageViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    setCurrentIndicator(position);
                }
            });
        } else {
            dotsIndicator.setVisibility(View.GONE);
        }
    }

    private void setupDotsIndicator(int count) {
        ImageView[] dots = new ImageView[count];
        dotsIndicator.removeAllViews();
        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_inactive));
            dots[i].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            dotsIndicator.addView(dots[i]);
        }
    }

    private void setCurrentIndicator(int position) {
        for (int i = 0; i < dotsIndicator.getChildCount(); i++) {
            ImageView imageView = (ImageView) dotsIndicator.getChildAt(i);
            imageView.setImageDrawable(ContextCompat.getDrawable(this,
                    i == position ? R.drawable.dot_active : R.drawable.dot_inactive
            ));
        }
    }

    private void setRatingBar(ProgressBar progressBar, TextView txtPercent, int percent) {
        progressBar.setProgress(percent);
        txtPercent.setText(String.format(Locale.getDefault(), "%d%%", percent));
    }

    private void toggleFavorite() {
        Log.d(TAG, "toggleFavorite() called");
        Integer userId = sessionManager.getUserId();
        Log.d(TAG, "User ID: " + userId);

        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        if (trip == null) {
            Log.e(TAG, "Trip is null, cannot toggle favorite");
            return;
        }

        // Use the trip ID directly (matches database trip_id column)
        int tripId = trip.getId();
        Log.d(TAG, "Trip ID: " + tripId + ", Route ID: " + trip.getRouteId() + ", Current isFavorite: " + isFavorite);

        if (isFavorite) {
            favoriteTripDatabase.removeFavoriteTrip(tripId, userId);
            isFavorite = false;
            Log.d(TAG, "Removed from favorites");
            Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
        } else {
            favoriteTripDatabase.addFavoriteTrip(trip, userId);
            isFavorite = true;
            Log.d(TAG, "Added to favorites");
            Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
        }

        // Update the icon directly based on the new state
        int iconRes = isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border;
        Log.d(TAG, "Setting icon resource: " + (isFavorite ? "ic_favorite_filled" : "ic_favorite_border"));
        btnFavorite.setImageResource(iconRes);
    }

    private void updateFavoriteIcon() {
        if (trip == null) {
            Log.e(TAG, "Trip is null, cannot update favorite icon");
            return;
        }

        Integer userId = sessionManager.getUserId();
        if (userId != null) {
            // Use the trip ID directly (matches database trip_id column)
            int tripId = trip.getId();
            isFavorite = favoriteTripDatabase.isFavorite(tripId, userId);
            Log.d(TAG, "updateFavoriteIcon() - Trip ID: " + tripId + ", Route ID: " + trip.getRouteId() + ", isFavorite: " + isFavorite);
        } else {
            isFavorite = false;
            Log.d(TAG, "updateFavoriteIcon() - User not logged in, setting isFavorite to false");
        }

        int iconRes = isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border;
        Log.d(TAG, "Setting icon resource: " + (isFavorite ? "ic_favorite_filled" : "ic_favorite_border"));
        btnFavorite.setImageResource(iconRes);
    }
}
