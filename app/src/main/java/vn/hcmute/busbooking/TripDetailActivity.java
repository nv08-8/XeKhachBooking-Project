package vn.hcmute.busbooking;

import android.content.Intent;
import android.os.Bundle;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.activity.SeatSelectionActivity;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.model.Trip;

public class TripDetailActivity extends AppCompatActivity {

    private ViewPager2 imageViewPager;
    private LinearLayout dotsIndicator;
    private ImageSliderAdapter imageSliderAdapter;
    private List<String> busImages;
    private ImageView[] dots;

    private TextView txtOperatorName, txtBusType, txtRoute;
    private TextView txtDepartureTime, txtDepartureDate, txtDepartureLocation;
    private TextView txtArrivalTime, txtArrivalDate, txtArrivalLocation;
    private TextView txtDuration;
    private TextView txtRating, txtReviews;
    private ProgressBar progressBar5, progressBar4, progressBar3, progressBar2, progressBar1;
    private TextView txtPercent5, txtPercent4, txtPercent3, txtPercent2, txtPercent1;
    private ViewGroup layoutAmenities; // use generic ViewGroup to avoid ClassCastException
    private ViewGroup layoutTimeline;  // use generic ViewGroup to avoid ClassCastException
    private Button btnBookNow;
    private ImageView btnBack, btnShare; // layout uses ImageView for back
    private TextView txtPrice;

    private Trip trip;
    private int tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        initViews();

        // Get trip data from intent
        trip = getIntent().getParcelableExtra("trip");

        if (trip != null) {
            displayBasicTripInfo();
            loadTripDetails();
        } else {
            Toast.makeText(this, "Không thể tải thông tin chuyến đi", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    /**
     * Runtime-safe finder that returns View. Caller should check instanceof before casting.
     */
    private View findViewByName(String name) {
        int id = getResources().getIdentifier(name, "id", getPackageName());
        if (id != 0) {
            return findViewById(id);
        }
        return null;
    }

    private void initViews() {
        // Use runtime lookup for optional/missing view IDs so compilation doesn't fail
        progressBar5 = asProgressBar(findViewByName("progressBar5"));
        progressBar4 = asProgressBar(findViewByName("progressBar4"));
        progressBar3 = asProgressBar(findViewByName("progressBar3"));
        progressBar2 = asProgressBar(findViewByName("progressBar2"));
        progressBar1 = asProgressBar(findViewByName("progressBar1"));
        txtPercent5 = asTextView(findViewByName("txtPercent5"));
        txtPercent4 = asTextView(findViewByName("txtPercent4"));
        txtPercent3 = asTextView(findViewByName("txtPercent3"));
        txtPercent2 = asTextView(findViewByName("txtPercent2"));
        txtPercent1 = asTextView(findViewByName("txtPercent1"));

        View a = findViewByName("layoutAmenities");
        if (a instanceof ViewGroup) layoutAmenities = (ViewGroup) a;
        View t = findViewByName("layoutTimeline");
        if (t instanceof ViewGroup) layoutTimeline = (ViewGroup) t;

        txtDepartureTime = asTextView(findViewByName("txtDepartureTime"));
        txtDepartureDate = asTextView(findViewByName("txtDepartureDate"));
        txtDepartureLocation = asTextView(findViewByName("txtDepartureLocation"));
        txtArrivalTime = asTextView(findViewByName("txtArrivalTime"));
        txtArrivalDate = asTextView(findViewByName("txtArrivalDate"));
        txtArrivalLocation = asTextView(findViewByName("txtArrivalLocation"));
        txtDuration = asTextView(findViewByName("txtDuration"));
        txtReviews = asTextView(findViewByName("txtReviews"));

        txtOperatorName = asTextView(findViewByName("txtOperatorName"));
        txtBusType = asTextView(findViewByName("txtBusType"));
        txtRoute = asTextView(findViewByName("txtRoute"));
        txtRating = asTextView(findViewByName("txtRating"));
        View back = findViewByName("btnBack");
        if (back instanceof ImageView) btnBack = (ImageView) back; else btnBack = null;
        View priceV = findViewByName("txtPrice");
        if (priceV instanceof TextView) txtPrice = (TextView) priceV;
    }

    // Safe cast helpers
    private TextView asTextView(View v) { return (v instanceof TextView) ? (TextView) v : null; }
    private ProgressBar asProgressBar(View v) { return (v instanceof ProgressBar) ? (ProgressBar) v : null; }

    private void displayBasicTripInfo() {
        // Display data from Trip object immediately
        if (trip == null) return;

        // Operator and bus type
        txtOperatorName.setText(trip.getOperator() != null ? trip.getOperator() : "");
        txtBusType.setText(trip.getBusType() != null ? trip.getBusType() : "");

        // Route
        txtRoute.setText(getString(R.string.route_format, trip.getFromLocation(), trip.getToLocation()));

        // Times
        String departureTime = trip.getDepartureTime();
        if (departureTime != null) {
            String[] depParts = formatDateTime(departureTime);
            if (txtDepartureTime != null) txtDepartureTime.setText(depParts[0]);
            if (txtDepartureDate != null) txtDepartureDate.setText(depParts[1]);
            if (txtDepartureLocation != null) txtDepartureLocation.setText(getString(R.string.bus_station_format, trip.getFromLocation()));
        }

        String arrivalTime = trip.getArrivalTime();
        if (arrivalTime != null) {
            String[] arrParts = formatDateTime(arrivalTime);
            if (txtArrivalTime != null) txtArrivalTime.setText(arrParts[0]);
            if (txtArrivalDate != null) txtArrivalDate.setText(arrParts[1]);
            if (txtArrivalLocation != null) txtArrivalLocation.setText(getString(R.string.bus_station_format, trip.getToLocation()));
        }

        // Duration
        if (trip.getDurationHours() != null && trip.getDurationHours() > 0 && txtDuration != null) {
            int hours = trip.getDurationHours().intValue();
            txtDuration.setText(getString(R.string.duration_hours_format, hours));
        }

        // Price
        if (trip.getPrice() > 0 && txtPrice != null) {
            txtPrice.setText(String.format(Locale.getDefault(), "%,.0f ₫", trip.getPrice()));
        }

        // Load bus image
        loadBusImage(trip.getOperator(), trip.getBusType());
    }

    private void loadTripDetails() {
        // Skip API call for now and display mock data
        // TODO: Implement getTripDetails in ApiService if needed
        displayMockData();
    }

    private void displayMockData() {
        // Display mock amenities based on bus type
        String busType = trip.getBusType();
        if (busType != null) {
            if (busType.toLowerCase().contains("limousine")) {
                addAmenityIfAvailable("Wifi miễn phí", true);
                addAmenityIfAvailable("Nước uống", true);
                addAmenityIfAvailable("Cổng sạc", true);
                addAmenityIfAvailable("TV", true);
                addAmenityIfAvailable("Điều hòa", true);
                if (busType.toLowerCase().contains("wc")) {
                    addAmenityIfAvailable("WC trên xe", true);
                }
            } else {
                addAmenityIfAvailable("Điều hòa", true);
                addAmenityIfAvailable("Wifi miễn phí", true);
                addAmenityIfAvailable("Nước uống", true);
                if (busType.toLowerCase().contains("wc")) {
                    addAmenityIfAvailable("WC trên xe", true);
                }
            }
        }

        // Display mock timeline
        displayMockTimeline();

        // Display mock rating
        if (txtRating != null) txtRating.setText("4.5");
        if (txtReviews != null) txtReviews.setText(getString(R.string.reviews_count, 1283));
        setRatingBar(progressBar5, txtPercent5, 50);
        setRatingBar(progressBar4, txtPercent4, 35);
        setRatingBar(progressBar3, txtPercent3, 10);
        setRatingBar(progressBar2, txtPercent2, 3);
        setRatingBar(progressBar1, txtPercent1, 2);
    }

    private void displayMockTimeline() {
        if (layoutTimeline == null) return;
        layoutTimeline.removeAllViews();

        // Add departure
        addTimelineStop("Bến xe " + trip.getFromLocation(),
                       trip.getDepartureTime(),
                       "Điểm khởi hành",
                       true);

        // Add rest stop if long trip (> 4 hours)
        if (trip.getDurationHours() > 4) {
            addTimelineStop("Trạm dừng chân Madagui",
                           null,
                           "Nghỉ 30 phút",
                           false);
        }

        // Add arrival
        addTimelineStop("Bến xe " + trip.getToLocation(),
                       trip.getArrivalTime(),
                       "Điểm đến",
                       false);
    }

    private void addTimelineStop(String location, String time, String description, boolean isFirst) {
        View stopView = getLayoutInflater().inflate(R.layout.item_timeline_stop, layoutTimeline, false);

        TextView txtLocation = stopView.findViewById(R.id.txtStopLocation);
        TextView txtTime = stopView.findViewById(R.id.txtStopTime);
        TextView txtDescription = stopView.findViewById(R.id.txtStopDescription);
        View lineTop = stopView.findViewById(R.id.lineTop);

        txtLocation.setText(location);
        if (time != null) {
            String[] parts = formatDateTime(time);
            txtTime.setText(parts[0]);
        } else {
            txtTime.setText("");
        }
        txtDescription.setText(description);

        // Hide top line for first item
        if (isFirst) {
            lineTop.setVisibility(View.INVISIBLE);
        }

        layoutTimeline.addView(stopView);
    }

    @SuppressWarnings("unchecked")
    private void displayTripDetails(Map<String, Object> data) {
        // Operator and bus type
        String operator = (String) data.get("operator");
        String busType = (String) data.get("bus_type");

        if (txtOperatorName != null) txtOperatorName.setText(operator != null ? operator : "");
        if (txtBusType != null) txtBusType.setText(busType != null ? busType : "");

        // Route
        String origin = (String) data.get("origin");
        String destination = (String) data.get("destination");
        if (txtRoute != null && origin != null && destination != null) {
            txtRoute.setText(getString(R.string.route_format, origin, destination));
        }

        // Times and locations
        String departureTime = (String) data.get("departure_time");
        String arrivalTime = (String) data.get("arrival_time");

        if (departureTime != null) {
            String[] depParts = formatDateTime(departureTime);
            if (txtDepartureTime != null) txtDepartureTime.setText(depParts[0]);
            if (txtDepartureDate != null) txtDepartureDate.setText(depParts[1]);
            if (txtDepartureLocation != null && origin != null) {
                txtDepartureLocation.setText(getString(R.string.bus_station_format, origin));
            }
        }

        if (arrivalTime != null) {
            String[] arrParts = formatDateTime(arrivalTime);
            if (txtArrivalTime != null) txtArrivalTime.setText(arrParts[0]);
            if (txtArrivalDate != null) txtArrivalDate.setText(arrParts[1]);
            if (txtArrivalLocation != null && destination != null) {
                txtArrivalLocation.setText(getString(R.string.bus_station_format, destination));
            }
        }

        // Duration
        String durationDisplay = (String) data.get("duration_display");
        if (durationDisplay != null && txtDuration != null) {
            txtDuration.setText(durationDisplay);
        }

        // Price
        Object priceObj = data.get("price");
        if (priceObj != null && txtPrice != null) {
            double price = ((Number) priceObj).doubleValue();
            txtPrice.setText(String.format(Locale.getDefault(), "%,.0f ₫", price));
        }

        // Load bus image
        loadBusImage(operator, busType);

        // Amenities
        Object amenitiesObj = data.get("amenities");
        if (amenitiesObj instanceof Map) {
            displayAmenities((Map<String, Object>) amenitiesObj);
        }

        // Timeline
        Object timelineObj = data.get("timeline");
        if (timelineObj instanceof List) {
            displayTimeline((List<Map<String, Object>>) timelineObj);
        }

        // Rating
        Object ratingObj = data.get("rating");
        if (ratingObj instanceof Map) {
            displayRating((Map<String, Object>) ratingObj);
        }
    }

    private String[] formatDateTime(String isoString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(isoString);

            if (date != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

                return new String[] {
                    timeFormat.format(date),
                    dateFormat.format(date)
                };
            }
        } catch (ParseException e) {
            // Silently fall through to default
        }
        return new String[] { "N/A", "N/A" };
    }

    private void loadBusImage(String operator, String busType) {
        android.util.Log.d("TripDetailActivity", "loadBusImage called - operator: " + operator + ", busType: " + busType);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getBusImage(operator, busType).enqueue(new Callback<Map<String, Object>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                android.util.Log.d("TripDetailActivity", "API Response - success: " + response.isSuccessful() + ", body: " + response.body());

                if (response.isSuccessful() && response.body() != null) {
                    // Get all images from response
                    Object allImagesObj = response.body().get("all_images");
                    List<String> allImages = (allImagesObj instanceof List) ? (List<String>) allImagesObj : null;
                    android.util.Log.d("TripDetailActivity", "All images: " + allImages);

                    if (allImages != null && !allImages.isEmpty()) {
                        busImages = allImages;
                        android.util.Log.d("TripDetailActivity", "Using all_images, count: " + allImages.size());
                    } else {
                        // Fallback to single image
                        String singleImage = (String) response.body().get("image");
                        android.util.Log.d("TripDetailActivity", "Fallback to single image: " + singleImage);
                        busImages = new java.util.ArrayList<>();
                        if (singleImage != null && !singleImage.isEmpty() && !singleImage.equals("placeholder")) {
                            busImages.add(singleImage);
                        }
                    }

                    if (busImages == null || busImages.isEmpty()) {
                        android.util.Log.w("TripDetailActivity", "No images available from API");
                        // Don't setup slider if no images
                        hideImageSlider();
                    } else {
                        setupImageSlider();
                    }
                } else {
                    android.util.Log.e("TripDetailActivity", "API response not successful or body is null");
                    hideImageSlider();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                android.util.Log.e("TripDetailActivity", "API call failed: " + t.getMessage(), t);
                hideImageSlider();
            }
        });
    }

    private void hideImageSlider() {
        // imageViewPager and dotsIndicator don't exist - no-op
        /*
        runOnUiThread(() -> {
            if (imageViewPager != null) {
                imageViewPager.setVisibility(View.GONE);
            }
            if (dotsIndicator != null) {
                dotsIndicator.setVisibility(View.GONE);
            }
        });
        */
    }

    private void setupImageSlider() {
        // imageViewPager doesn't exist - commented out
        /*
        if (busImages == null || busImages.isEmpty() || imageViewPager == null) {
            hideImageSlider();
            return;
        }

        runOnUiThread(() -> {
            // Show views
            imageViewPager.setVisibility(View.VISIBLE);
            if (dotsIndicator != null) {
                dotsIndicator.setVisibility(View.VISIBLE);
            }

            // Setup adapter
            imageSliderAdapter = new ImageSliderAdapter(busImages);
            imageViewPager.setAdapter(imageSliderAdapter);

            // Setup dots indicator - only show if more than 1 image
            if (busImages.size() > 1) {
                setupDotsIndicator(busImages.size());
                dotsIndicator.setVisibility(android.view.View.VISIBLE);
            } else {
                dotsIndicator.setVisibility(android.view.View.GONE);
            }

            // Set current indicator
            setCurrentIndicator(0);

            // Page change callback
            imageViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    setCurrentIndicator(position);
                }
            });
        });
        */
    }

    private void setupDotsIndicator(int count) {
        // dotsIndicator doesn't exist - commented out
        /*
        if (dotsIndicator == null) return;
        dots = new ImageView[count];
        dotsIndicator.removeAllViews();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);

        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_inactive));
            dots[i].setLayoutParams(params);
            dotsIndicator.addView(dots[i]);
        }
        */
    }

    private void setCurrentIndicator(int position) {
        // dots don't exist - commented out
        /*
        if (dots == null || position >= dots.length) {
            return;
        }

        for (int i = 0; i < dots.length; i++) {
            int drawableId = (i == position) ? R.drawable.dot_active : R.drawable.dot_inactive;
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, drawableId));
        }
        */
    }

    private void displayAmenities(Map<String, Object> amenities) {
        if (layoutAmenities == null) return;
        layoutAmenities.removeAllViews();

        addAmenityIfAvailable("Wifi miễn phí", (amenities.get("wifi") instanceof Boolean) ? (Boolean) amenities.get("wifi") : false);
        addAmenityIfAvailable("Nước uống", (amenities.get("water") instanceof Boolean) ? (Boolean) amenities.get("water") : false);
        addAmenityIfAvailable("Cổng sạc", (amenities.get("charging") instanceof Boolean) ? (Boolean) amenities.get("charging") : false);
        addAmenityIfAvailable("Điều hòa", (amenities.get("ac") instanceof Boolean) ? (Boolean) amenities.get("ac") : false);
        addAmenityIfAvailable("TV", (amenities.get("tv") instanceof Boolean) ? (Boolean) amenities.get("tv") : false);
        addAmenityIfAvailable("WC trên xe", (amenities.get("wc") instanceof Boolean) ? (Boolean) amenities.get("wc") : false);
    }

    private void addAmenityIfAvailable(String name, Boolean available) {
        if (available == null || !available) return;
        if (layoutAmenities == null) return;
        View amenityView = getLayoutInflater().inflate(R.layout.item_amenity, layoutAmenities, false);
        TextView txtAmenity = amenityView.findViewById(R.id.txtAmenity);
        if (txtAmenity != null) txtAmenity.setText(name);
        layoutAmenities.addView(amenityView);
    }

    private void displayTimeline(List<Map<String, Object>> timeline) {
        if (layoutTimeline == null) return;
        layoutTimeline.removeAllViews();

        for (int i = 0; i < timeline.size(); i++) {
            Map<String, Object> stop = timeline.get(i);
            View stopView = getLayoutInflater().inflate(R.layout.item_timeline_stop, layoutTimeline, false);

            TextView txtTime = stopView.findViewById(R.id.txtStopTime);
            TextView txtLocation = stopView.findViewById(R.id.txtStopLocation);
            TextView txtDescription = stopView.findViewById(R.id.txtStopDescription);
            View lineTop = stopView.findViewById(R.id.lineTop);

            String time = (String) stop.get("time");
            if (time != null && txtTime != null) {
                txtTime.setText(formatDateTime(time)[0]);
            }

            if (txtLocation != null) txtLocation.setText((String) stop.get("location"));
            if (txtDescription != null) txtDescription.setText((String) stop.get("description"));

            if (i == 0 && lineTop != null) lineTop.setVisibility(View.INVISIBLE);
            layoutTimeline.addView(stopView);
        }
    }

    @SuppressWarnings("unchecked")
    private void displayRating(Map<String, Object> rating) {
        Object avgObj = rating.get("average");
        Object totalObj = rating.get("total_reviews");

        if (avgObj != null && txtRating != null) {
            double average = ((Number) avgObj).doubleValue();
            txtRating.setText(String.format(Locale.getDefault(), "%.1f", average));
        }

        if (totalObj != null && txtReviews != null) {
            int total = ((Number) totalObj).intValue();
            txtReviews.setText(String.format(Locale.getDefault(), "%,d reviews", total));
        }

        Object breakdownObj = rating.get("breakdown");
        if (breakdownObj instanceof Map) {
            Map<String, Object> breakdown = (Map<String, Object>) breakdownObj;
            setRatingBar(progressBar5, txtPercent5, (Number) breakdown.get("5"));
            setRatingBar(progressBar4, txtPercent4, (Number) breakdown.get("4"));
            setRatingBar(progressBar3, txtPercent3, (Number) breakdown.get("3"));
            setRatingBar(progressBar2, txtPercent2, (Number) breakdown.get("2"));
            setRatingBar(progressBar1, txtPercent1, (Number) breakdown.get("1"));
        }
    }

    private void setRatingBar(ProgressBar progressBar, TextView txtPercent, Number value) {
        if (value != null && progressBar != null && txtPercent != null) {
            int percent = value.intValue();
            progressBar.setProgress(percent);
            txtPercent.setText(String.format(Locale.getDefault(), "%d%%", percent));
        }
    }

    private void shareTrip() {
        if (trip == null) return;

        String shareText = String.format(Locale.getDefault(),
            "Chuyến xe %s → %s\nNhà xe: %s\nGiờ khởi hành: %s\nGiá: %s",
            trip.getOrigin(),
            trip.getDestination(),
            trip.getOperator(),
            trip.getDepartureTime(),
            String.format(Locale.getDefault(), "%,.0f ₫", trip.getPrice())
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ chuyến đi"));
    }
}

