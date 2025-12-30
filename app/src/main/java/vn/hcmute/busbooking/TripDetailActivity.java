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
import java.util.HashMap;
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
    private ApiService apiService;
    private SessionManager sessionManager;
    private boolean isFavorite = false;
    private int tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);
        initViews();

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        // Get data from intent
        tripId = getIntent().getIntExtra("trip_id", 0);
        trip = getIntent().getParcelableExtra("trip");

        if (trip != null) {
            // Show basic info first if object is passed
            if (tripId == 0) tripId = trip.getId();
            displayBasicTripInfo();
        }

        if (tripId != 0) {
            // Fetch trip details from API
            fetchTripDetails(tripId);
        } else if (trip == null) {
            Toast.makeText(this, "Không có thông tin chuyến đi", Toast.LENGTH_SHORT).show();
            finish();
        }

        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setOnClickListener(v -> finish());
        }
        btnFavorite.setOnClickListener(v -> toggleFavorite());
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

    private void fetchTripDetails(int tripId) {
        Log.d(TAG, "Fetching trip details for ID: " + tripId);
        apiService.getTripDetails(tripId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Log.d(TAG, "Response received - Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    // Use the constructor that handles the map directly to avoid Gson's issues with nested objects
                    trip = new Trip(response.body());
                    displayBasicTripInfo();
                    updateAdditionalTripDetails(response.body());
                    checkIfFavorite();
                } else {
                    Log.e(TAG, "Response not successful or body is null. Code: " + response.code());
                    if (trip == null) {
                        Toast.makeText(TripDetailActivity.this, "Không thể tải thông tin chuyến đi", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch trip details", t);
                if (trip == null) {
                    Toast.makeText(TripDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayBasicTripInfo() {
        if (trip == null) return;
        Log.d(TAG, "=== displayBasicTripInfo() START ===");

        if (trip.getOperator() != null) {
            txtOperatorName.setText(trip.getOperator());
        }
        if (trip.getBusType() != null) {
            txtBusType.setText(trip.getBusType());
        }

        txtPrice.setText(String.format(Locale.getDefault(), "%,.0f ₫", trip.getPrice()));

        btnBookNow.setOnClickListener(v -> {
            Intent intent = new Intent(TripDetailActivity.this, SeatSelectionActivity.class);
            intent.putExtra("trip", trip);
            startActivity(intent);
        });

        setupImageSliderWithPlaceholder();
        Log.d(TAG, "=== displayBasicTripInfo() END ===");
    }

    private void updateAdditionalTripDetails(Map<String, Object> data) {
        Object imageUrlsObj = data.get("bus_image_url");
        List<String> imageUrls = new ArrayList<>();
        if (imageUrlsObj instanceof String) {
            Type listType = new TypeToken<ArrayList<String>>() {
            }.getType();
            imageUrls = new Gson().fromJson((String) imageUrlsObj, listType);
        } else if (imageUrlsObj instanceof List) {
            imageUrls.addAll((List<String>) imageUrlsObj);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            imageUrls = imageUrls.stream()
                    .filter(url -> url != null && !url.contains("tiktok.com"))
                    .collect(Collectors.toList());
        }

        if (!imageUrls.isEmpty()) {
            imageViewPager.setVisibility(View.VISIBLE);
            dotsIndicator.setVisibility(View.VISIBLE);
            setupImageSlider(imageUrls);
        }

        if (data.get("amenities") instanceof Map) {
            displayAmenities((Map<String, Boolean>) data.get("amenities"));
        }

        if (data.get("timeline") instanceof List) {
            displayTimeline((List<Map<String, Object>>) data.get("timeline"));
        }

        if (data.get("reviews") instanceof List) {
            displayReviews((List<Map<String, Object>>) data.get("reviews"));
        }

        if (data.get("seat_layout") != null) {
            String seatLayoutJson = new Gson().toJson(data.get("seat_layout"));
            trip.setSeatLayout(seatLayoutJson);
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
        int[] ratingCounts = new int[5];

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
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = inputFormat.parse(isoString);
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "";
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

        int tripId = trip.getId();
        
        if (isFavorite) {
            apiService.removeFavorite(userId, tripId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        isFavorite = false;
                        updateFavoriteIcon();
                        Toast.makeText(TripDetailActivity.this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(TripDetailActivity.this, "Lỗi khi xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Toast.makeText(TripDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Map<String, Integer> body = new HashMap<>();
            body.put("trip_id", tripId);
            apiService.addFavorite(userId, body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        isFavorite = true;
                        updateFavoriteIcon();
                        Toast.makeText(TripDetailActivity.this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(TripDetailActivity.this, "Lỗi khi thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Toast.makeText(TripDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkIfFavorite() {
        Integer userId = sessionManager.getUserId();
        if (userId != null && trip != null) {
            apiService.getFavorites(userId).enqueue(new Callback<List<Trip>>() {
                @Override
                public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Trip favoriteTrip : response.body()) {
                            if (favoriteTrip.getId() == trip.getId()) {
                                isFavorite = true;
                                break;
                            }
                        }
                        updateFavoriteIcon();
                    }
                }

                @Override
                public void onFailure(Call<List<Trip>> call, Throwable t) {
                    // Do nothing
                }
            });
        } else {
            updateFavoriteIcon();
        }
    }

    private void updateFavoriteIcon() {
        int iconRes = isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border;
        btnFavorite.setImageResource(iconRes);
    }
}
